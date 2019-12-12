package rabbit.open.dtx.common.nio.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerEventHandler;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

/**
 * nio server
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class DtxServer {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private Selector readSelector;

    //服务器监听通道
    private ServerSocketChannel listenChannel;

    // 监听线程
    private Thread listener;

    private boolean close = false;

    // boss线程等待信号
    private Semaphore semaphore = new Semaphore(0);

    // 网络事件接口
    private AbstractServerEventHandler netEventHandler;

    private int errCount = 0;

    // 错误阈值
    private int errCountThreshold = 100;

    public DtxServer(int port, AbstractServerEventHandler netEventHandler) throws IOException {
        this.netEventHandler = netEventHandler;
        this.netEventHandler.setDtxServer(this);
        readSelector = Selector.open();
        listenChannel = ServerSocketChannel.open();
        // 异步模式
        listenChannel.configureBlocking(false);
        listenChannel.socket().bind(new InetSocketAddress(port));
        //注册接收事件
        listenChannel.register(readSelector, SelectionKey.OP_ACCEPT);
        listener = new Thread(() -> {
            logger.info("dtx server is listening on port: {}", port);
            while (true) {
                try {
                    if (0 == readSelector.select()) {
                        errCount++;
                    }
                    Iterator<SelectionKey> keys = readSelector.selectedKeys().iterator();
                    handleRequest(keys);
                    if (close) {
                        break;
                    }
                    epollBugDetection();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
    }

    /**
     * epoll 空循环bug检测
     * @author  xiaoqianbin
     * @date    2019/12/13
     **/
    private void epollBugDetection() throws IOException {
        if (errCount < errCountThreshold) {
            return;
        }
        logger.error("epoll bug is detected, errCount: {}", errCount);
        errCount = 0;
        Selector newSelector = Selector.open();
        for (SelectionKey key : readSelector.keys()) {
            if (!key.isValid()) {
                continue;
            }
            int ops = key.interestOps();
            key.channel().register(newSelector, ops, key.attachment());
            key.cancel();
        }
        closeResource(readSelector);
        readSelector = newSelector;
    }

    /**
     * 启动nio server
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public void start() {
        listener.start();
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
    public void closeChannelKey(SelectionKey key) {
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
        SelectionKey key = channel.register(readSelector, SelectionKey.OP_READ);
        ChannelAgent agent = new ChannelAgent(key);
        key.attach(agent);
        netEventHandler.onConnected(agent);
    }

    /**
     * 安全关闭 nio server
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public void shutdown() {
        logger.info("dtx server is closing....");
        close = true;
        try {
            readSelector.wakeup();
            listener.join();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        closeAllSelectionKeys();
        netEventHandler.onServerClosed();
        closeResource(listenChannel);
        closeResource(readSelector);
        logger.info("dtx server is closed!");
    }

    private void closeAllSelectionKeys() {
        for (SelectionKey key : readSelector.keys()) {
            closeChannelKey(key);
        }
    }

    private void closeResource(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
