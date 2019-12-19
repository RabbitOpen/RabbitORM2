package rabbit.open.dtx.common.nio.client.ext;

import rabbit.open.dtx.common.nio.client.*;
import rabbit.open.dtx.common.nio.exception.NetworkException;
import rabbit.open.dtx.common.nio.exception.RpcException;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.NetEventHandler;
import rabbit.open.dtx.common.nio.pub.NioSelector;
import rabbit.open.dtx.common.nio.pub.protocol.Application;
import rabbit.open.dtx.common.nio.pub.protocol.CommitMessage;
import rabbit.open.dtx.common.nio.pub.protocol.RollBackMessage;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.*;

/**
 * 客户端连接池
 * @author xiaoqianbin
 * @date 2019/12/8
 **/
public class DtxChannelAgentPool extends AbstractResourcePool<ChannelAgent> {

    protected ArrayBlockingQueue<Node> nodes = new ArrayBlockingQueue<>(256);

    protected NioSelector nioSelector;

    protected Thread readThread;

    protected boolean run = true;

    private NetEventHandler netEventHandler = new ClientNetEventHandler(this);

    private DistributedTransactionManager transactionManger;

    // 消息监听器
    private Map<Class<?>, MessageListener> listenerMap = new ConcurrentHashMap<>();

    // 监控线程
    private AgentMonitor monitor = new AgentMonitor("client-agent-pool-monitor", this);

    // 链接channel注册任务
    private ArrayBlockingQueue<FutureTask<SelectionKey>> channelRegistryTasks = new ArrayBlockingQueue<>(100);

    public DtxChannelAgentPool(DistributedTransactionManager transactionManger) throws IOException {
        this.transactionManger = transactionManger;
        initListeners(transactionManger);
        nodes.addAll(transactionManger.getServerNodes());
        nioSelector = new NioSelector(Selector.open());
        readThread = new Thread(() -> {
            while (run) {
                try {
                    read();
                } catch (Exception e) {
                    logger.info(e.getMessage(), e);
                }
            }
        }, "client-channel-selector");
        readThread.start();
        initConnections();
        monitor.start();
    }

    private void initListeners(DistributedTransactionManager transactionManger) {
        if (null != transactionManger.getMessageListener()) {
            listenerMap.put(CommitMessage.class, transactionManger.getMessageListener());
            listenerMap.put(RollBackMessage.class, transactionManger.getMessageListener());
        }
    }

    public Map<Class<?>, MessageListener> getListenerMap() {
        return listenerMap;
    }

    /**
     * 初始化到节点之间的连接(与所有可用的服务都建立一个连接)
     * @author  xiaoqianbin
     * @date    2019/12/16
     **/
    public void initConnections() {
        for (int i = 0; i < nodes.size(); i++) {
            tryCreateResource();
        }
    }

    public DistributedTransactionManager getTransactionManger() {
        return transactionManger;
    }

    public FutureTask<SelectionKey> addTask(Callable<SelectionKey> task) throws InterruptedException {
        FutureTask<SelectionKey> selectionKeyTask = new FutureTask<>(task);
        this.channelRegistryTasks.put(selectionKeyTask);
        return selectionKeyTask;
    }

    /**
     * 读数据
     * @author  xiaoqianbin
     * @date    2019/12/8
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
            if (channel.finishConnect()) {
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
     * @author  xiaoqianbin
     * @date    2019/12/16
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

    @Override
    protected ChannelAgent newResource() {
        for (Node node : nodes) {
            if (node.isIdle() && !node.isIsolated()) {
                try {
                    ChannelAgent agent = new ChannelAgent(node, this);
                    node.setIdle(false);
                    // 注册连接销毁回调事件
                    agent.addShutdownHook(() -> releaseAgentResource(node, agent));
                    // 通报应用名
                    agent.send(new Application(getTransactionManger().getApplicationName())).getData();
                    logger.info("{} created a new connection, current size {}", transactionManger.getApplicationName(), count + 1);
                    return agent;
                } catch (NetworkException e) {
                    node.setIsolated(true);
                    throw e;
                } catch (Exception e) {
                    throw new NetworkException(e);
                }
            }
        }
        throw new RpcException("can't create connection any more");
    }

    /**
     * 销毁agent时回收对应的资源
     * @param	node
	 * @param	agent
     * @author  xiaoqianbin
     * @date    2019/12/16
     **/
    private void releaseAgentResource(Node node, ChannelAgent agent) {
        try {
            node.setIdle(true);
            destroyResource();
            agent.closeQuietly(agent.getSelectionKey().channel());
            agent.getSelectionKey().cancel();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        monitor.wakeup();
    }

    @Override
    public void gracefullyShutdown() {
        logger.info("{} is closing.....", DtxChannelAgentPool.class.getSimpleName());
        run = false;
        try {
            monitor.shutdown();
            releaseResources();
            readThread.join();
            nioSelector.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("{} gracefullyShutdown", DtxChannelAgentPool.class.getSimpleName());
    }

    // 回收所有资源
    private void releaseResources() throws InterruptedException {
        while (0 != count) {
            ChannelAgent agent = queue.poll(3, TimeUnit.SECONDS);
            if (null != agent) {
                agent.destroy();
            }
        }
    }

    @Override
    protected boolean isRunning() {
        return run;
    }

    public NioSelector getNioSelector() {
        return nioSelector;
    }
}