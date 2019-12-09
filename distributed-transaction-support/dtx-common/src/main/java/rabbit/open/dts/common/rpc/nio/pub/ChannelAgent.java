package rabbit.open.dts.common.rpc.nio.pub;

import rabbit.open.dts.common.rpc.nio.client.PooledResource;
import rabbit.open.dts.common.utils.ext.KryoObjectSerializer;

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
    public final void response(Serializable data, Long requestId) throws IOException {
        send(data, DataType.RESPONSE, ResponseStatus.SUCCESS, requestId);
    }

    /***
     * 返回异常数据
     * @param   exception
     * @param   requestId
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public final void error(Exception exception, Long requestId) throws IOException {
        send(exception, DataType.RESPONSE, ResponseStatus.EXCEPTION, requestId);
    }

    /**
     * 发送请求数据
     * @param data
     * @param requestId
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public final void request(Serializable data, Long requestId) throws IOException {
        send(data, DataType.REQUEST, ResponseStatus.SUCCESS, requestId);
    }

    /**
     * 发送数据
     * @param data
     * @param dataType
     * @param responseStatus
     * @param requestId
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    private final void send(Serializable data, int dataType, int responseStatus, Long requestId) throws IOException {
        try {
            lock.lock();
            active();
            ProtocolData protocolData = new ProtocolData();
            protocolData.setDataType(dataType);
            protocolData.setData(data);
            protocolData.setResponseStatus(responseStatus);
            protocolData.setRequestId(requestId);
            byte[] bytes = new KryoObjectSerializer().serialize(protocolData);
            ByteBuffer buffer = ByteBuffer.allocate(bytes.length + ProtocolData.PROTOCOL_HEADER_LENGTH);
            buffer.putInt(bytes.length);
            buffer.put(bytes);
            buffer.position(0);
            SocketChannel channel = (SocketChannel) selectionKey.channel();
            while (buffer.position() != buffer.capacity()) {
                channel.write(buffer);
            }
        } finally {
            lock.unlock();
        }
    }
}
