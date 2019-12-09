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

    // 网络事件接口
    private AbstractServerEventHandler netEventHandler;

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
    }

    /**
     * 启动nio server
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public synchronized void start() {
        if (null != listener) {
            return;
        }
        listener = new Thread(() -> {
            while (true) {
                try {
                    Iterator<SelectionKey> keys = selectKeys(1000);
                    handleRequest(keys);
                    if (close) {
                        break;
                    }
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }
            }
        });
        listener.start();
    }

    private void handleRequest(Iterator<SelectionKey> keys) {
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
                    // disable read事件
                    key.interestOps(0);
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
    }

    /**
     * 唤醒selector
     * @author  xiaoqianbin
     * @date    2019/12/7
     **/
    public void wakeup() {
        readSelector.wakeup();
    }

    /**
     * 关闭连接
     * @param	key
     * @author  xiaoqianbin
     * @date    2019/12/7
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
     * @author  xiaoqianbin
     * @date    2019/12/7
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
     * 获取当前已经处理IO就绪的keys
     * @throws IOException
     */
    private Iterator<SelectionKey> selectKeys(long timeoutMilliseconds) throws IOException {
        readSelector.select(timeoutMilliseconds);
        return readSelector.selectedKeys().iterator();
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
            listener.join();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
        netEventHandler.onServerClosed();
        closeResource(listenChannel);
        closeResource(readSelector);
        logger.info("dtx server is closed!");
    }

    private void closeResource(Closeable closeable) {
        try {
            closeable.close();
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

}
