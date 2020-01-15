package rabbit.open.dtx.common.nio.client.ext;

import rabbit.open.dtx.common.context.DistributedTransactionContext;
import rabbit.open.dtx.common.exception.*;
import rabbit.open.dtx.common.nio.client.*;
import rabbit.open.dtx.common.nio.pub.CallHelper;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.NioSelector;
import rabbit.open.dtx.common.nio.pub.protocol.*;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.ConnectException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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

    protected AbstractTransactionManager transactionManger;

    private Map<Class<?>, Object> proxyCache = new ConcurrentHashMap<>();

    // 消息监听器
    private Map<Class<?>, MessageListener> listenerMap = new ConcurrentHashMap<>();

    private static final AtomicLong THREAD_ID = new AtomicLong(0);

    // 监控线程
    protected AgentMonitor monitor = new AgentMonitor("client-agent-pool-monitor-" + THREAD_ID.getAndAdd(1L), this);

    // 链接channel注册任务
    private ArrayBlockingQueue<FutureTask<SelectionKey>> channelRegistryTasks = new ArrayBlockingQueue<>(64);

    // 客户端实例 id
    protected Long instanceId = null;

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
        listenerMap.put(ClusterMeta.class, msg -> refreshServerNodes((ClusterMeta) msg));
    }

    /**
     * 唤醒monitor，尝试修复已经隔离的节点
     * @author  xiaoqianbin
     * @date    2020/1/10
     **/
    public void wakeUpMonitor() {
        monitor.wakeup();
    }

    /**
     * 刷新节点信息
     * @param	msg
     * @author  xiaoqianbin
     * @date    2020/1/7
     **/
    protected void refreshServerNodes(ClusterMeta msg) {
        boolean foundNewNodes = false;
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
            	foundNewNodes = true;
                node.setIsolated(false);
                node.setIdle(true);
                CallHelper.ignoreExceptionCall(() -> nodes.put(node));
            }
        }
        if (foundNewNodes) {
        	logger.info("refreshServerNodes: {}", msg.getNodes());
        	wakeUpMonitor();
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
        makeSureNodeAvailable();
        for (int i = 0; i < nodes.size(); i++) {
            try {
                tryCreateResource();
            } catch (NoActiveNodeException e) {
                logger.error(e.getMessage());
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
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
            if (node.isIdle() && !node.isIsolated() && run) {
                return true;
            }
        }
        return false;
    }

    /**
     * 确保节点是未被隔离的
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
        ChannelAgent agent = createChannelAgent();
        try {
            loadInstanceId(agent);
            // 通报应用名
            reportAppInfo(agent);
            return agent;
        } catch (Exception e) {
            agent.destroy();
            throw new DtxException(e);
        }
    }

    private ChannelAgent createChannelAgent() {
        for (Node node : nodes) {
            if (node.isIdle() && !node.isIsolated() && run) {
                ChannelAgent agent = new ChannelAgent(node, this);
                if (!agent.isConnected()) {
                    node.setIsolated(true);
                    node.setIdle(true);
                    agent.destroy();
                    logger.error("connect node[{}:{}] failed", node.getHost(), node.getPort());
                    continue;
                }
                node.setIdle(false);
                node.bindAgent(agent);
                logger.info("{} created a new connection, current size {}", transactionManger.getApplicationName(), getResourceCount() + 1);
                return agent;
            }
        }
        throw new NoActiveNodeException();
    }

    /**
     * 通报app信息
     * @param	agent
     * @author  xiaoqianbin
     * @date    2020/1/7
     **/
    protected void reportAppInfo(ChannelAgent agent) throws InterruptedException {
        ClusterMeta meta = (ClusterMeta) agent.send(generateAppInfo()).getData(3L);
        if (null == meta) {
            return;
        }
        refreshServerNodes(meta);
    }

    /**
     * 生成需要汇报的app信息
     * @author  xiaoqianbin
     * @date    2020/1/10
     **/
    protected Application generateAppInfo() {
        return new Application(transactionManger.getApplicationName(), instanceId);
    }

    /**
     * 加载客户端应用信息
     * @param	agent
     * @author  xiaoqianbin
     * @date    2020/1/9
     **/
    private synchronized void loadInstanceId(ChannelAgent agent) throws InterruptedException {
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
        if (agent.isClosed()) {
            return;
        }
        node.setIdle(true);
        agent.closeQuietly(agent.getSelectionKey().channel());
        agent.getSelectionKey().cancel();
        removeResource(agent);
    }

    @Override
    public void gracefullyShutdown() {
        try {
            createLock.lock();
            logger.info("{} is closing.....", DtxChannelAgentPool.class.getSimpleName());
            run = false;
            monitor.shutdown();
            // 先关闭listener 后释放连接，因为有些异步listener会使用连接进行response
            closeAllListeners();
            closeAllResources();
            nioSelector.wakeup();
            readThread.join();
            nioSelector.close();
            logger.info("{} gracefully shutdown", DtxChannelAgentPool.class.getSimpleName());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            createLock.unlock();
        }
    }

    // 关闭所有的listener
    private void closeAllListeners() {
        for (MessageListener listener : listenerMap.values()) {
            if (listener instanceof AbstractMessageListener) {
                AbstractMessageListener abstractMessageListener = (AbstractMessageListener) listener;
                abstractMessageListener.close();
            }
        }
    }

    // 回收所有资源
    private void closeAllResources() {
        while (!roundList.isEmpty()) {
            ChannelAgent agent = roundList.browse();
            if (null != agent) {
                agent.destroy();
            }
        }
    }

    public NioSelector getNioSelector() {
        return nioSelector;
    }

    /**
     * 接口代理类
	 * @param	rpcTimeoutSeconds
     * @author  xiaoqianbin
     * @date    2020/1/9
     **/
    @SuppressWarnings("unchecked")
	public synchronized <T> T proxy(Class<T> clz, long rpcTimeoutSeconds) {
        if (!proxyCache.containsKey(clz)) {
            InvocationHandler handler = new JdkProxy(this, rpcTimeoutSeconds, clz);
            Object instance = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{clz}, handler);
            proxyCache.put(clz, instance);
        }
        return (T) proxyCache.get(clz);
    }

    /**
     * 接口代理类
     * @param	clz
     * @author  xiaoqianbin
     * @date    2020/1/9
     **/
    public <T> T proxy(Class<T> clz) {
        return this.proxy(clz, 5L);
    }

    private class JdkProxy implements InvocationHandler {

        DtxChannelAgentPool pool;

        String namespace;

        private long rpcTimeoutSeconds;

        private ThreadLocal<AtomicInteger> retryContext = new ThreadLocal<>();

        public JdkProxy(DtxChannelAgentPool pool, long rpcTimeoutSeconds, Class<?> clz) {
            this.rpcTimeoutSeconds = rpcTimeoutSeconds;
            this.pool = pool;
            namespace = clz.getName();
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if ("equals".equals(method.getName())) {
                return true;
            }
            if ("hashCode".equals(method.getName())) {
                return -1;
            }
            if ("toString".equals(method.getName())) {
                return namespace;
            }
            try {
                retryContext.set(new AtomicInteger(0));
                return doInvoke(method, args);
            } finally {
                retryContext.remove();
            }
        }

        private Object doInvoke(Method method, Object[] args) {
            Object data;
            ChannelAgent agent = null;
            try {
                RpcProtocol protocol = new RpcProtocol(namespace, method.getName(), method.getParameterTypes(), args);
                agent = pool.getResource();
                FutureResult result = agent.send(protocol);
                agent.release();
                Long timeout = DistributedTransactionContext.getRollbackTimeout();
                data = result.getData(null == timeout ? rpcTimeoutSeconds : timeout);
            } catch (GetConnectionTimeoutException e) {
                throw e;
            } catch (NetworkException | TimeoutException e) {
                if (retryContext.get().addAndGet(1) >= pool.getNodes().size()) {
                    logger.error("invoke failed！retried {}", retryContext.get().get());
                    throw e;
                } else {
                    logger.warn("retry for timeout error");
                    return retry(method, args, agent);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new DtxException(e);
            }
            if (data instanceof DtxException) {
                throw (DtxException) data;
            }
            return data;
        }

        private Object retry(Method method, Object[] args, ChannelAgent agent) {
            if (null != agent) {
                agent.destroy();
            }
            // 网络IO异常就直接重试
            return doInvoke(method, args);
        }
    }

}
