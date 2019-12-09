package rabbit.open.dtx.common.nio.pub;

import rabbit.open.dtx.common.nio.client.PooledResource;
import rabbit.open.dtx.common.nio.exception.NetworkException;
import rabbit.open.dtx.common.utils.ext.KryoObjectSerializer;

import java.io.IOException;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 通道代理对象
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class ChannelAgent {

    public static final int ERROR = 1;

    public static final int NORMAL = 0;

    private SelectionKey selectionKey;

    private ByteBuffer dataBuffer;

    private String remote;

    private ReentrantLock lock = new ReentrantLock();

    private PooledResource resource;

    // 等待agent正确连接上
    private Semaphore semaphore = new Semaphore(0);

    // 连接上次活跃时间
    private long lastActiveTime = 0;

    public ChannelAgent(SelectionKey selectionKey) throws IOException {
        this.selectionKey = selectionKey;
        SocketChannel sc = (SocketChannel) selectionKey.channel();
        if (null != sc.getRemoteAddress()) {
            remote = sc.getRemoteAddress().toString();
        }
        active();
    }

    public ChannelAgent(SelectionKey selectionKey, PooledResource resource) throws IOException {
        this(selectionKey);
        this.resource = resource;
    }

    public PooledResource getResource() {
        return resource;
    }

    public void ensureConnected() throws InterruptedException {
        semaphore.acquire();
    }

    public void connected() {
        semaphore.release();
    }

    public long getLastActiveTime() {
        return lastActiveTime;
    }

    public void active() {
        lastActiveTime = System.currentTimeMillis();
    }


    public SelectionKey getSelectionKey() {
        return selectionKey;
    }

    public ByteBuffer getDataBuffer(int bufferSize) {
        if (null == this.dataBuffer) {
            dataBuffer = ByteBuffer.allocate(bufferSize);
        }
        return dataBuffer;
    }

    public String getRemote() {
        return remote;
    }

    /**
     * 正常返回数据
     * @param data
     * @param requestId
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public final void response(Serializable data, Long requestId) {
        send(data, requestId);
    }

    /***
     * 返回异常数据
     * @param   exception
     * @param   requestId
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public final void responseError(Exception exception, Long requestId) {
        send(exception, requestId, true);
    }

    /**
     * 发送请求数据
     * @param data
     * @param requestId
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public final void request(Object data, Long requestId) {
        send(data, requestId);
    }

    /**
     * 发送数据
     * @param data
     * @param requestId
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    private final void send(Object data, Long requestId) {
        send(data, requestId, false);
    }
    /**
     * 发送数据
     * @param data
     * @param requestId
     * @param error 异常数据
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    private final void send(Object data, Long requestId, boolean error) {
        try {
            lock.lock();
            active();
            ProtocolData protocolData = new ProtocolData();
            protocolData.setData(data);
            protocolData.setRequestId(requestId);
            byte[] bytes = new KryoObjectSerializer().serialize(protocolData);
            ByteBuffer buffer = ByteBuffer.allocate(bytes.length + ProtocolData.PROTOCOL_HEADER_LENGTH);
            buffer.putInt(bytes.length);
            if (error) {
                buffer.putInt(ERROR);
            } else {
                buffer.putInt(NORMAL);
            }
            buffer.put(bytes);
            buffer.position(0);
            SocketChannel channel = (SocketChannel) selectionKey.channel();
            while (buffer.position() != buffer.capacity()) {
                channel.write(buffer);
            }
        } catch (Exception e) {
            throw new NetworkException(e);
        } finally {
            lock.unlock();
        }
    }
}
