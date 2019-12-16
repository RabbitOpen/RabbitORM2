package rabbit.open.dtx.common.nio.client.ext;

import rabbit.open.dtx.common.nio.client.AbstractResourcePool;
import rabbit.open.dtx.common.nio.client.DistributedTransactionManager;
import rabbit.open.dtx.common.nio.client.Node;
import rabbit.open.dtx.common.nio.exception.NetworkException;
import rabbit.open.dtx.common.nio.exception.RpcException;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.NetEventHandler;
import rabbit.open.dtx.common.nio.pub.NioSelector;
import rabbit.open.dtx.common.nio.pub.protocol.Application;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * 客户端连接池
 * @author xiaoqianbin
 * @date 2019/12/8
 **/
public class DtxChannelAgentPool extends AbstractResourcePool<ChannelAgent> {

    protected List<Node> nodes;

    protected NioSelector nioSelector;

    protected Thread readThread;

    protected boolean run = true;

    private NetEventHandler netEventHandler = new ClientNetEventHandler(this);

    private DistributedTransactionManager transactionManger;

    // 链接channel注册任务
    private ArrayBlockingQueue<FutureTask<SelectionKey>> channelRegistryTasks = new ArrayBlockingQueue<>(100);

    public DtxChannelAgentPool(DistributedTransactionManager transactionManger) throws IOException {
        this.transactionManger = transactionManger;
        this.nodes = new ArrayList<>(transactionManger.getServerNodes());
        nioSelector = new NioSelector(Selector.open());
        readThread = new Thread(() -> {
            while (run) {
                try {
                    read();
                } catch (Exception e) {
                    logger.info(e.getMessage(), e);
                }
            }
        });
        readThread.start();
        initConnections();
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
                    agent.addShutdownHook(() -> {
                        try {
                            node.setIdle(true);
                            destroyResource();
                            agent.closeQuietly(agent.getSelectionKey().channel());
                            agent.getSelectionKey().cancel();
                        } catch (Exception e) {
                            logger.error(e.getMessage(), e);
                        }
                    });
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

    @Override
    public void gracefullyShutdown() {
        logger.info("{} is closing.....", DtxChannelAgentPool.class.getSimpleName());
        run = false;
        try {
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
