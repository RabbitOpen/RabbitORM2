package rabbit.open.dtx.common.nio.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.NioSelector;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerEventHandler;
import rabbit.open.dtx.common.utils.NodeIdHelper;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;

/**
 * nio server
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class DtxServer {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private NioSelector nioSelector;

    //服务器监听通道
    private ServerSocketChannel listenChannel;

    // io数据监听线程
    private Thread ioListener;

    // 死连接监控
    private ServerAgentMonitor serverAgentMonitor;

    private boolean close = false;

    // boss线程等待信号
    private Semaphore semaphore = new Semaphore(0);

    // 网络事件接口
    private AbstractServerEventHandler netEventHandler;

    private static final AtomicLong SELECTOR_ID = new AtomicLong(0);

    private String serverId;

    private String hostName;
    private int port;

    public DtxServer(int port, AbstractServerEventHandler netEventHandler) throws IOException {
        this("localhost", port, netEventHandler);
    }

    public DtxServer(String hostName, int port, AbstractServerEventHandler netEventHandler) throws IOException {
        this.port = port;
        this.hostName = hostName;
        this.netEventHandler = netEventHandler;
        this.netEventHandler.setDtxServer(this);
        nioSelector = new NioSelector(Selector.open());
        listenChannel = ServerSocketChannel.open();
        // 异步模式
        listenChannel.configureBlocking(false);
        listenChannel.socket().bind(new InetSocketAddress(this.hostName, this.port));
        //注册接收事件
        listenChannel.register(nioSelector.getRealSelector(), SelectionKey.OP_ACCEPT);
        serverId = NodeIdHelper.calcServerId(InetAddress.getLocalHost().getHostAddress(), port);
        ioListener = new Thread(() -> {
            logger.info("dtx server is listening on port: {}", port);
            while (true) {
                try {
                    nioSelector.select();
                    Iterator<SelectionKey> keys = nioSelector.selectedKeys().iterator();
                    handleRequest(keys);
                    if (close) {
                        break;
                    }
                    nioSelector.epollBugDetection();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }, "event-selector-" + SELECTOR_ID.getAndAdd(1L));
        serverAgentMonitor = new ServerAgentMonitor("server-agent-monitor-" + SELECTOR_ID.getAndAdd(1L));
        serverAgentMonitor.start();
    }

    public ServerAgentMonitor getServerAgentMonitor() {
        return serverAgentMonitor;
    }

    public AbstractServerEventHandler getNetEventHandler() {
        return netEventHandler;
    }

    public List<ChannelAgent> getAgents() {
        return new ArrayList<>(serverAgentMonitor.getAgents());
    }

    public String getServerId() {
        return serverId;
    }

    /**
     * 启动nio server
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public void start() {
        ioListener.start();
    }

    private void handleRequest(Iterator<SelectionKey> keys) throws InterruptedException {
        int cnt = 0;
        while (keys.hasNext()) {
            SelectionKey key = keys.next();
            try {
                keys.remove();
                if (!key.isValid()) {
                    continue;
                }
                if (key.isAcceptable()) {
                    acceptConnection();
                }
                if (key.isReadable()) {
                    cnt++;
                    ChannelAgent agent = (ChannelAgent) key.attachment();
                    netEventHandler.onDataReceived(agent);
                }
            } catch (CancelledKeyException e) {
                closeChannelKey(key);
            } catch (Exception e) {
                closeChannelKey(key);
                logger.error(e.getMessage(), e);
            }
        }
        semaphore.acquire(cnt);
    }

    /**
     * 唤醒selector
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public void wakeup() {
        semaphore.release();
    }

    /**
     * 关闭连接
     * @param key
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    private void closeChannelKey(SelectionKey key) {
        try {
            key.channel().close();
            key.cancel();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * 处理连接请求
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    private void acceptConnection() throws IOException {
        SocketChannel channel = listenChannel.accept();
        channel.configureBlocking(false);
        // 把此channel 客户端对象作为一个事件注册到 选择器 selector中
        SelectionKey key = channel.register(nioSelector.getRealSelector(), SelectionKey.OP_READ);
        ChannelAgent agent = new ChannelAgent(key);
        serverAgentMonitor.registerMonitor(agent);
        agent.addShutdownHook(() -> {
            serverAgentMonitor.unRegister(agent);
            closeChannelKey(key);
        });
        key.attach(agent);
        netEventHandler.onConnected(agent);
    }

    /**
     * 安全关闭 nio server
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public void shutdown() {
        logger.info("dtx server[{}:{}] is closing....", hostName, port);
        close = true;
        try {
            serverAgentMonitor.shutdown();
            nioSelector.wakeup();
            ioListener.join();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        closeAllSelectionKeys();
        netEventHandler.onServerClosed();
        NioSelector.closeResource(listenChannel);
        NioSelector.closeResource(nioSelector);
        logger.info("dtx server[{}:{}] is closed!", hostName, port);
    }

    private void closeAllSelectionKeys() {
        for (SelectionKey key : nioSelector.keys()) {
            closeChannelKey(key);
        }
    }

}
