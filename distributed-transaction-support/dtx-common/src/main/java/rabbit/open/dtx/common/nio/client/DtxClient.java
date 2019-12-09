package rabbit.open.dtx.common.nio.client;

import rabbit.open.dtx.common.nio.client.ext.DtxResourcePool;
import rabbit.open.dtx.common.nio.exception.NetworkException;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;

import java.io.Closeable;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * nio 客户端
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class DtxClient implements PooledResource {

    // 连接的服务器
    private Node node;

    private Selector selector;

    private DtxResourcePool pool;

    private ChannelAgent channelAgent;

    private static final AtomicLong packageIdGenerator = new AtomicLong(0);

    // 候车间
    private static Map<Long, FutureResult> waitingRoom = new ConcurrentHashMap<>();

    private SocketChannel channel;

    public DtxClient(Node node, DtxResourcePool pool) {
        try {
            this.pool = pool;
            this.node = node;
            this.selector = pool.getSelector();
            channel = SocketChannel.open();
            // 设置通道为非阻塞
            channel.configureBlocking(false);
            SelectionKey key = channel.register(this.selector, SelectionKey.OP_CONNECT);
            channelAgent = new ChannelAgent(key, this);
            key.attach(channelAgent);
            // 客户端连接服务器,其实方法执行并没有实现连接
            channel.connect(new InetSocketAddress(this.node.getHost(), this.node.getPort()));
            channelAgent.ensureConnected();
        } catch (Exception e) {
            closeQuietly(channel);
            throw new NetworkException(e);
        }
    }

    /**
     * 发送数据
     * @param	data
     * @author  xiaoqianbin
     * @date    2019/12/8
     **/
    public FutureResult send(Object data) {
        Long id = packageIdGenerator.getAndAdd(1);
        FutureResult result = new FutureResult();
        waitingRoom.put(id, result);
        channelAgent.request(data, id);
        return result;
    }

    public static long getLeftMessages() {
        return waitingRoom.size();
    }

    /**
     * 找出候车间那个人
     * @param	id
     * @author  xiaoqianbin
     * @date    2019/12/8
     **/
    public static FutureResult findFutureResult(Long id) {
        return waitingRoom.remove(id);
    }

    private void closeQuietly(Closeable c) {
        try {
            if (null != c) {
                c.close();
            }
        } catch (Exception e) {
            // TO DO :ignore
        }
    }

    @Override
    public void destroy() {
        this.pool.destroyResource();
        closeQuietly(channelAgent.getSelectionKey().channel());
        channelAgent.getSelectionKey().cancel();
    }

    /**
     * 释放连接资源
     * @author  xiaoqianbin
     * @date    2019/12/8
     **/
    @Override
    public void release() {
        this.pool.release(this);
    }

}
