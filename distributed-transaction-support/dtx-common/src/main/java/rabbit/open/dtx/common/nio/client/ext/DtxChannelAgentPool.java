package rabbit.open.dtx.common.nio.client.ext;

import rabbit.open.dtx.common.exception.NetworkException;
import rabbit.open.dtx.common.exception.NoActiveNodeException;
import rabbit.open.dtx.common.nio.client.*;
import rabbit.open.dtx.common.nio.pub.CallHelper;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.NioSelector;
import rabbit.open.dtx.common.nio.pub.protocol.*;

import java.io.IOException;
import java.net.ConnectException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 客户端连接池
 * @author xiaoqianbin
 * @date 2019/12/8
 **/
@SuppressWarnings("rawtypes")
public class DtxChannelAgentPool extends AbstractResourcePool<ChannelAgent> {

    protected ArrayBlockingQueue<Node> nodes = new ArrayBlockingQueue<>(256);

    protected NioSelector nioSelector;

    protected Thread readThread;

    protected boolean run = true;

    private ClientNetEventHandler netEventHandler;

    private AbstractTransactionManager transactionManger;

    // 消息监听器
    private Map<Class<?>, MessageListener> listenerMap = new ConcurrentHashMap<>();

    private static final AtomicLong THREAD_ID = new AtomicLong(0);

    private ThreadLocal<ChannelAgent> agentContext = new ThreadLocal<>();

    private ReentrantLock closeLock = new ReentrantLock();

    // 监控线程
    private AgentMonitor monitor = new AgentMonitor("client-agent-pool-monitor-" + THREAD_ID.getAndAdd(1L), this);

    // 链接channel注册任务
    private ArrayBlockingQueue<FutureTask<SelectionKey>> channelRegistryTasks = new ArrayBlockingQueue<>(64);

    // 客户端实例 id
    private Long instanceId = null;

    public DtxChannelAgentPool(AbstractTransactionManager transactionManger) throws IOException {
        this(transactionManger, null, null);
    }

    public DtxChannelAgentPool(AbstractTransactionManager transactionManger, ClientNetEventHandler netEventHandler, Map<Class<?>, MessageListener> listeners) throws IOException {
        this.transactionManger = transactionManger;
        initNetEventHandler(netEventHandler);
        initListeners(listeners);
        nodes.addAll(transactionManger.getServerNodes());
        nioSelector = new NioSelector(Selector.open());
        readThread = new Thread(() -> {
            while (run) {
                try {
                    read();
                } catch (ConnectException e) {
                    logger.error(e.getMessage());
                } catch (Exception e) {
                    logger.info(e.getMessage(), e);
                }
            }
        }, "client-channel-selector-" + THREAD_ID.getAndAdd(1L));
        readThread.start();
        initConnections();
        monitor.start();
    }

    public ArrayBlockingQueue<Node> getNodes() {
        return nodes;
    }

    /**
     * <b>@description 初始化网络事件处理器 </b>
     * @param netEventHandler
     */
    protected void initNetEventHandler(ClientNetEventHandler netEventHandler) {
        if (null == netEventHandler) {
            this.netEventHandler = new ClientNetEventHandler(this);
        } else {
            this.netEventHandler = netEventHandler;
            this.netEventHandler.setChannelAgentPool(this);
        }
    }

    private void initListeners(Map<Class<?>, MessageListener> listeners) {
        if (null != transactionManger.getMessageListener()) {
            listenerMap.put(CommitMessage.class, transactionManger.getMessageListener());
            listenerMap.put(RollBackMessage.class, transactionManger.getMessageListener());
        }
        if (null != listeners) {
            listenerMap.putAll(listeners);
        }
        // 服务端广播节点
        listenerMap.put(ClusterMeta.class, msg -> {
            refreshServerNodes((ClusterMeta) msg);
            monitor.wakeup();
        });
    }

    /**
     * 刷新节点信息
     * @param	msg
     * @author  xiaoqianbin
     * @date    2020/1/7
     **/
    public void refreshServerNodes(ClusterMeta msg) {
        logger.info("refreshServerNodes: {}", msg.getNodes());
        ClusterMeta meta = msg;
        // 更新现有节点的隔离状态
        for (Node node : nodes) {
            if (nodeExist(node, meta.getNodes())) {
                node.setIsolated(false);
            } else {
                node.setIsolated(true);
            }
        }
        // 刷新节点的列表信息
        for (Node node : meta.getNodes()) {
            if (!nodeExist(node, nodes)) {
                node.setIsolated(false);
                CallHelper.ignoreExceptionCall(() -> nodes.put(node));
            }
        }
    }

    /**
     * 判断节点在不在列表nodes中
     * @param    node
     * @param    nodes
     * @author xiaoqianbin
     * @date 2019/12/27
     **/
    private boolean nodeExist(Node node, Collection<Node> nodes) {
        for (Node existedNode : nodes) {
            if (node.getId().equals(existedNode.getId())) {
                return true;
            }
        }
        return false;
    }

    public Map<Class<?>, MessageListener> getListenerMap() {
        return listenerMap;
    }

    /**
     * 初始化到节点之间的连接(与所有可用的服务都建立一个连接)
     * @author xiaoqianbin
     * @date 2019/12/16
     **/
    public void initConnections() {
        if (closeLock.tryLock()) {
            try {
                doInitialize();
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            } finally {
                closeLock.unlock();
            }
        }
    }

    private void doInitialize() {
        makeSureNodeAvailable();
        for (int i = 0; i < nodes.size(); i++) {
            try {
                tryCreateResource();
            } finally {
                ChannelAgent agent = agentContext.get();
                if (null != agent) {
                    agentContext.remove();
                    doRequest(agent);
                }
            }
        }
    }

    public FutureTask<SelectionKey> addTask(Callable<SelectionKey> task) throws InterruptedException {
        FutureTask<SelectionKey> selectionKeyTask = new FutureTask<>(task);
        this.channelRegistryTasks.put(selectionKeyTask);
        return selectionKeyTask;
    }

    /**
     * 读数据
     * @author xiaoqianbin
     * @date 2019/12/8
     **/
    private void read() throws IOException {
        nioSelector.select();
        executeRegistryTask();
        Iterator<?> iterator = nioSelector.selectedKeys().iterator();
        while (iterator.hasNext()) {
            SelectionKey key = (SelectionKey) iterator.next();
            iterator.remove();
            if (!key.isValid()) {
                continue;
            }
            SocketChannel channel = (SocketChannel) key.channel();
            boolean connect;
            try {
                connect = channel.finishConnect();
            } catch (Exception e) {
                ChannelAgent agent = (ChannelAgent) key.attachment();
                agent.connectFailed();
                throw e;
            }
            if (connect) {
                ChannelAgent agent = (ChannelAgent) key.attachment();
                channel.register(nioSelector.getRealSelector(), SelectionKey.OP_READ);
                // 重新attach
                key.attach(agent);
                agent.connected();
            }
            if (key.isReadable()) {
                ChannelAgent agent = (ChannelAgent) key.attachment();
                netEventHandler.onDataReceived(agent);
            }
        }
        nioSelector.epollBugDetection();
    }

    private void executeRegistryTask() {
        while (!channelRegistryTasks.isEmpty()) {
            Runnable task = channelRegistryTasks.poll();
            nioSelector.reduceErrorCount();
            task.run();
        }
    }

    /**
     * 只要有未被隔离的空闲节点，就可以新建连接
     * @author xiaoqianbin
     * @date 2019/12/16
     **/
    @Override
    protected boolean canCreate() {
        for (Node node : nodes) {
            if (node.isIdle() && !node.isIsolated()) {
                return true;
            }
        }
        return false;
    }

    /**
     * 确保至少有一个节点是未被隔离的
     * @author  xiaoqianbin
     * @date    2020/1/6
     **/
    private void makeSureNodeAvailable() {
        for (Node node : nodes) {
            // 保证有一个节点是未被隔离的即可
            node.setIsolated(false);
        }
    }

    @Override
    protected ChannelAgent newResource() {
        for (Node node : nodes) {
            if (node.isIdle() && !node.isIsolated() && run) {
                try {
                    ChannelAgent agent = new ChannelAgent(node, this);
                    if (!agent.isConnected()) {
                        node.setIsolated(true);
                        // 因为没有添加到池里，所以直接close
                        agent.close();
                        logger.error("connect node[{}:{}] failed", node.getHost(), node.getPort());
                        continue;
                    }
                    node.setIdle(false);
                    node.bindAgent(agent);
                    logger.info("{} created a new connection, current size {}", transactionManger.getApplicationName(), count.get() + 1);
                    agentContext.set(agent);
                    return agent;
                } catch (NetworkException e) {
                    node.setIsolated(true);
                    node.setIdle(true);
                    throw e;
                } catch (Exception e) {
                    throw new NetworkException(e);
                }
            }
        }
        throw new NoActiveNodeException();
    }

    // 发送业务数据
    private void doRequest(ChannelAgent agent) {
        try {
            acquireInstanceId(agent);
            // 通报应用名
            reportAppInfo(agent);
        } catch (Exception e) {
            agent.destroy();
            logger.error(e.getMessage());
        }
    }

    /**
     * 通报app信息
     * @param	agent
     * @author  xiaoqianbin
     * @date    2020/1/7
     **/
    protected void reportAppInfo(ChannelAgent agent) throws InterruptedException {
        agent.send(new Application(transactionManger.getApplicationName(), instanceId)).getData(3L);
    }

    private void acquireInstanceId(ChannelAgent agent) throws InterruptedException {
        if (null == instanceId) {
            ClientInstance instance = (ClientInstance) agent.send(new ClientInstance()).getData(3L);
            instanceId = instance.getId();
            logger.info("load instance id --> {}", instanceId);
        }
    }

    /**
     * 销毁agent时回收对应的资源
     * @param node
     * @param agent
     * @author xiaoqianbin
     * @date 2019/12/16
     **/
    public void releaseAgentResource(Node node, ChannelAgent agent) {
        try {
            if (agent.isClosed()) {
                return;
            }
            node.setIdle(true);
            destroyResource(agent);
            agent.closeQuietly(agent.getSelectionKey().channel());
            agent.getSelectionKey().cancel();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    @Override
    public void gracefullyShutdown() {
        try {
            closeLock.lock();
            logger.info("{} is closing.....", DtxChannelAgentPool.class.getSimpleName());
            run = false;
            monitor.shutdown();
            // 先关闭listener 后释放连接，因为有些异步listener会使用连接进行response
            closeAllListener();
            releaseResources();
            readThread.join();
            nioSelector.close();
            logger.info("{} gracefully shutdown", DtxChannelAgentPool.class.getSimpleName());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            closeLock.unlock();
        }
    }

    // 关闭所有的listener
    private void closeAllListener() {
        for (MessageListener listener : listenerMap.values()) {
            if (listener instanceof AbstractMessageListener) {
                AbstractMessageListener abstractMessageListener = (AbstractMessageListener) listener;
                abstractMessageListener.close();
            }
        }
    }

    // 回收所有资源
    private void releaseResources() throws InterruptedException {
        while (0 != getResourceCount()) {
            ChannelAgent agent = queue.poll(3, TimeUnit.SECONDS);
            if (null != agent) {
                agent.destroy();
            }
        }
    }

    public NioSelector getNioSelector() {
        return nioSelector;
    }

}
