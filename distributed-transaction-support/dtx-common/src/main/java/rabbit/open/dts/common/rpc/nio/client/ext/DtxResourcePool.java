package rabbit.open.dts.common.rpc.nio.client.ext;

import rabbit.open.dts.common.rpc.nio.client.AbstractResourcePool;
import rabbit.open.dts.common.rpc.nio.client.DtxClient;
import rabbit.open.dts.common.rpc.nio.client.Node;
import rabbit.open.dts.common.rpc.nio.exception.NetworkException;
import rabbit.open.dts.common.rpc.nio.pub.ChannelAgent;
import rabbit.open.dts.common.rpc.nio.pub.NetEventHandler;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * 客户端连接池
 * @author xiaoqianbin
 * @date 2019/12/8
 **/
public class DtxResourcePool extends AbstractResourcePool<DtxClient> {

    protected List<Node> nodes;

    protected int nodeIndex = 0;

    protected Selector selector;

    protected Thread readThread;

    protected boolean run = true;

    private NetEventHandler netEventHandler = new ClientNetEventHandler();

    public DtxResourcePool(int maxSize, List<Node> nodes) throws IOException {
        super(maxSize);
        this.nodes = nodes;
        selector = Selector.open();
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
    }

    /**
     * 读数据
     * @author  xiaoqianbin
     * @date    2019/12/8
     **/
    private void read() throws IOException {
        selector.select(500);
        Iterator<?> iterator = selector.selectedKeys().iterator();
        while (iterator.hasNext()) {
            SelectionKey key = (SelectionKey) iterator.next();
            iterator.remove();
            if (!key.isValid()) {
                continue;
            }
            SocketChannel channel = (SocketChannel) key.channel();
            if (channel.finishConnect()) {
                ChannelAgent agent = (ChannelAgent) key.attachment();
                channel.register(selector, SelectionKey.OP_READ);
                // 重新attach
                key.attach(agent);
                agent.connected();
            }
            if (key.isReadable()) {
                key.interestOps(0);
                ChannelAgent agent = (ChannelAgent) key.attachment();
                netEventHandler.onDataReceived(agent);
            }
        }
    }

    /**
     * 创建连接时发现网络不通或者其它错误
     * @author xiaoqianbin
     * @date 2019/12/8
     **/
    @Override
    protected void onNetWorkException(NetworkException e) {
        // TO DO :网络异常时切换node
        logger.error(e.getMessage(), e);
    }

    @Override
    protected void onNetWorkRecovered() {
        // TO DO: 网络异常恢复
    }

    @Override
    protected DtxClient createResource() {
        logger.info("created a connection");
        return new DtxClient(nodes.get(nodeIndex), this);
    }

    @Override
    public void gracefullyShutdown() {
        logger.info("DtxResourcePool is closing.....");
        run = false;
        try {
            releaseResources();
            readThread.join();
            selector.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        logger.info("DtxResourcePool gracefullyShutdown");
    }

    // 回收所有资源
    private void releaseResources() throws InterruptedException {
        while (0 != count) {
            DtxClient client = queue.poll(3, TimeUnit.SECONDS);
            if (null != client) {
                client.destroy();
            }
        }
    }

    @Override
    protected boolean isRunning() {
        return run;
    }

    public Selector getSelector() {
        return selector;
    }
}
