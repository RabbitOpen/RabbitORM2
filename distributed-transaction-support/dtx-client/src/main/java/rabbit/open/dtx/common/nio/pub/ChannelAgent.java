package rabbit.open.dtx.common.nio.pub;

import rabbit.open.dtx.common.nio.client.FutureResult;
import rabbit.open.dtx.common.nio.client.Node;
import rabbit.open.dtx.common.nio.client.PooledResource;
import rabbit.open.dtx.common.nio.client.ext.DtxChannelAgentPool;
import rabbit.open.dtx.common.nio.exception.NetworkException;
import rabbit.open.dtx.common.utils.ext.KryoObjectSerializer;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 通道代理对象
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class ChannelAgent implements PooledResource {

    public static final int ERROR = 1;

    public static final int NORMAL = 0;

    private SelectionKey selectionKey;

    private ByteBuffer dataBuffer;

    private String remote;

    private ReentrantLock lock = new ReentrantLock();

    // 连接的服务器
    private Node node;

    private NioSelector selector;

    private DtxChannelAgentPool pool;

    private static final AtomicLong packageIdGenerator = new AtomicLong(0);

    // 候车间
    private static Map<Long, FutureResult> waitingRoom = new ConcurrentHashMap<>();

    private SocketChannel channel;

    // 等待agent正确连接上
    private Semaphore semaphore = new Semaphore(0);

    // 关闭回调
    private List<Runnable> shutdownHooks = new ArrayList<>();

    // 服务端地址
    private String serverAddr;

    // 应用名
    private String appName;

    // agent是否已经关闭
    private boolean closed = false;

    // 连接上次活跃时间
    private long lastActiveTime = 0;

    /**
     * 服务端新建agent
     * @param	selectionKey
     * @author  xiaoqianbin
     * @date    2019/12/16
     **/
    public ChannelAgent(SelectionKey selectionKey) throws IOException {
        this.selectionKey = selectionKey;
        SocketChannel sc = (SocketChannel) selectionKey.channel();
        if (null != sc.getRemoteAddress()) {
            remote = sc.getRemoteAddress().toString();
        }
        active();
    }

    /**
     * 客户端新建agent
     * @param	node
	 * @param	pool
     * @author  xiaoqianbin
     * @date    2019/12/16
     **/
    public ChannelAgent(Node node, DtxChannelAgentPool pool) {
        try {
            this.pool = pool;
            this.node = node;
            this.selector = pool.getNioSelector();
            channel = SocketChannel.open();
            // 设置通道为非阻塞
            channel.configureBlocking(false);
            FutureTask<SelectionKey> futureTask = pool.addTask(() -> channel.register(this.selector.getRealSelector(),
                    SelectionKey.OP_CONNECT));
            this.selector.wakeup();
            selectionKey = futureTask.get();
            selectionKey.attach(this);
            // 客户端连接服务器,其实方法执行并没有实现连接
            channel.connect(new InetSocketAddress(this.node.getHost(), this.node.getPort()));
            ensureConnected();
        } catch (Exception e) {
            closeQuietly(channel);
            if (null != selectionKey) {
                selectionKey.cancel();
            }
            throw new NetworkException(e);
        }
    }

    public void closeQuietly(Closeable c) {
        try {
            if (null != c) {
                c.close();
            }
        } catch (Exception e) {
            // TO DO :ignore
        }
    }

    public void ensureConnected() throws InterruptedException {
        semaphore.acquire();
    }

    public void connected() throws IOException {
        SocketChannel sc = (SocketChannel) getSelectionKey().channel();
        serverAddr = sc.getRemoteAddress().toString();
        semaphore.release();
    }

    public String getServerAddr() {
        return serverAddr;
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

    public void bindDataBuffer(ByteBuffer dataBuffer) {
		this.dataBuffer = dataBuffer;
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
    public final void response(Object data, Long requestId) {
        send(data, requestId);
    }

    /**
     * 空响应
     * @param	requestId
     * @author  xiaoqianbin
     * @date    2019/12/24
     **/
    public final void ack(Long requestId) {
        send(null, requestId);
    }

    /**
     * 通知消息
     * @param	data
     * @author  xiaoqianbin
     * @date    2019/12/24
     **/
    public final void notify(Object data) {
        send(data, null);
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
    private final void request(Object data, Long requestId) {
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
     * 客户端发送数据
     * @param    data
     * @author xiaoqianbin
     * @date 2019/12/8
     **/
    public FutureResult send(Object data) {
        Long id = packageIdGenerator.getAndAdd(1);
        FutureResult result = new FutureResult(() -> waitingRoom.remove(id));
        waitingRoom.put(id, result);
        request(data, id);
        return result;
    }

    /**
     * 发送数据
     * @param data
     * @param requestId
     * @param error     异常数据
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
            SocketChannel sc = (SocketChannel) selectionKey.channel();
            while (buffer.position() != buffer.capacity()) {
                sc.write(buffer);
            }
        } catch (Exception e) {
            throw new NetworkException(e);
        } finally {
            lock.unlock();
        }
    }

    public void addShutdownHook(Runnable hook) {
        try {
            lock.lock();
            shutdownHooks.add(hook);
        } finally {
            lock.unlock();
        }
    }

    // 销毁连接, 释放资源
    public void destroy() {
        try {
            lock.lock();
            for (Runnable hook : shutdownHooks) {
                hook.run();
            }
            closed = true;
        } finally {
            lock.unlock();
        }
    }

    @Override
	public void release() {
		try {
			lock.lock();
			if (!closed) {
				pool.release(this);
			}
				
		} finally {
			lock.unlock();
		}
	}

    public static long getLeftMessages() {
        return waitingRoom.size();
    }

    /**
     * 找出候车间那个人
     * @param    id
     * @author xiaoqianbin
     * @date 2019/12/8
     **/
    public static FutureResult findFutureResult(Long id) {
        return waitingRoom.remove(id);
    }

    public boolean isClosed() {
        return closed;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }
}
