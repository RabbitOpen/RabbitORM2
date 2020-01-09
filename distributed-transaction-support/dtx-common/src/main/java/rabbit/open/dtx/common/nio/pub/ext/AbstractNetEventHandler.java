package rabbit.open.dtx.common.nio.pub.ext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.common.exception.ClientClosedException;
import rabbit.open.dtx.common.exception.InvalidPackageSizeException;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.NetEventHandler;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.server.handler.DataDispatcher;
import rabbit.open.dtx.common.utils.ext.KryoObjectSerializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * 事件处理器
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public abstract class AbstractNetEventHandler implements NetEventHandler {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    // 数据接收缓冲区(默认的缓冲区只有4K, 接收大数据时会临时开辟一个大的缓冲区，用完后直接回收)
    private ThreadLocal<ByteBuffer> switchRegion = new ThreadLocal<>();

    // agent缓存
    private static final ThreadLocal<ChannelAgent> agentContext = new ThreadLocal<>();

    /**
     * 获取数据分发器
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    protected abstract DataDispatcher getDispatcher();

    @Override
    public void onConnected(ChannelAgent agent) {
        logger.info("{} connected!", agent.getRemote());
    }

    /**
     * 处理读事件
     * @param task
     * @author xiaoqianbin
     * @date 2019/12/8
     **/
    protected abstract void executeReadTask(Runnable task);

    /**
     * 唤醒读selector
     * @param agent
     * @author xiaoqianbin
     * @date 2019/12/8
     **/
    protected abstract void wakeUpSelector(ChannelAgent agent);

    /**
     * 默认缓冲区大小
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    protected int getDefaultBufferSize() {
        return 4 * 1024;
    }

    /**
     * 最大缓冲区大小
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    protected int getMaxBufferSize() {
        return 4 * 1024 * 1024;
    }

    @Override
    public void onDisconnected(ChannelAgent agent) {
        logger.info("{} closed!", agent.getRemote());
    }

    /**
     * 处理消息
     * @param agent
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    @Override
    public void onDataReceived(ChannelAgent agent) {
        agent.active();
        executeReadTask(() -> {
            try {
                cacheAgent(agent);
                // 再次激活，减低monitor误判死连接的几率
                agent.active();
                switchRegion.set(agent.getDataBuffer(getDefaultBufferSize()));
                readData(agent);
            } catch (Exception e) {
                agent.destroy();
                onDisconnected(agent);
                logWithException(e);
            } finally {
                wakeUpSelector(agent);
                switchRegion.remove();
                clearAgent();
            }
        });
    }

    private void logWithException(Exception e) {
        if (e instanceof ClientClosedException) {
            return;
        }
        if (e instanceof IOException) {
            logger.error(e.getMessage());
        } else {
            logger.error(e.getMessage(), e);
        }
    }

    protected void clearAgent() {
        agentContext.remove();
    }

    protected void cacheAgent(ChannelAgent agent) {
        agentContext.set(agent);
    }

    // 读数据
    private void readData(ChannelAgent agent) throws IOException {
        SocketChannel sc = (SocketChannel) agent.getSelectionKey().channel();
        while (true) {
            int len = sc.read(switchRegion.get());
            if (-1 == len) {
                throw new ClientClosedException("client is closed");
            }
            if (processData(agent)) {
                return;
            }
        }
    }

    /**
     * 获取当前请求业务的agent信息
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    public static ChannelAgent getCurrentAgent() {
        return agentContext.get();
    }

    /**
     * 解析数据
     * @param agent
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    private boolean processData(ChannelAgent agent) {
        ByteBuffer buffer = switchRegion.get();
        if (buffer.position() < ProtocolData.PROTOCOL_HEADER_LENGTH) {
            //数据头尚未读取完整
            return true;
        }
        int pos = buffer.position();
        buffer.position(0);
        int dataLength = buffer.getInt();
        int dataType = buffer.getInt();
        if (dataLength > buffer.capacity() - ProtocolData.PROTOCOL_HEADER_LENGTH) {
            assertPackageDataLength(dataLength);
            // 如果实际需要接收的数据超过了默认buffer长度, 则生成临时缓冲用来处理数据
            switchDataCache(pos, dataLength);
            // 将新分配的buffer绑定到agent
            agent.bindDataBuffer(switchRegion.get());
            logger.warn("generate temp buffer to process request data, buffer size [{}] : ", switchRegion.get().limit());
            return true;
        }
        if (packetReadOver(pos, dataLength)) {
            byte[] data = new byte[dataLength];
            buffer.get(data, 0, dataLength);
            buffer.compact();
            buffer.position(pos - dataLength - ProtocolData.PROTOCOL_HEADER_LENGTH);
            ProtocolData protocolData;
            if (ChannelAgent.NORMAL == dataType) {
                protocolData = new KryoObjectSerializer().deserialize(data, ProtocolData.class);
            } else {
                protocolData = new KryoObjectSerializer().deserialize(data, ProtocolData.class, true);
            }
            processData(protocolData, agent);
            if (0 == buffer.position()) {
            	resetAgentBufferAfterExpanding(agent, buffer.capacity());
            	// 处理完数据就直接返回
            	return true;
            }
            // 没处理完数据就继续检测，如果还有数据就继续处理，数据不够就一帧就挂起客户端
            return processData(agent);
        } else {
            // reset position 直接挂起客户端，防止客户端io跟不上导致服务端挂起
            buffer.position(pos);
            return true;
        }
    }

    /**
     * <b>@description 扩容后用完buffer就回收 </b>
     * @param agent
     * @param currentBufferSize
     */
	protected void resetAgentBufferAfterExpanding(ChannelAgent agent, int currentBufferSize) {
		if (currentBufferSize != getDefaultBufferSize()) {
			// 如果扩过容就清理buffer，释放内容
			agent.bindDataBuffer(null);
		}
	}

    /**
     * 处理协议数据
     * @param protocolData
     * @param agent
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    protected abstract void processData(ProtocolData protocolData, ChannelAgent agent);

    /**
     * 读取到完整的包
     * @param pos        buffer的position
     * @param dataLength 数据的长度
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    private boolean packetReadOver(int pos, int dataLength) {
        return pos >= ProtocolData.PROTOCOL_HEADER_LENGTH + dataLength;
    }

    /**
     * 检测包长度
     * @param dataLength
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    private void assertPackageDataLength(int dataLength) {
        if (dataLength > getMaxBufferSize()) {
            throw new InvalidPackageSizeException(dataLength, getMaxBufferSize());
        }
    }

    /**
     * 如果实际需要接收的数据超过了默认buffer长度, 则生成临时缓冲用来处理数据
     * @param pos
     * @param dataLength
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    private void switchDataCache(int pos, int dataLength) {
        ByteBuffer buffer = switchRegion.get();
        buffer.position(0);
        buffer.limit(pos);
        ByteBuffer newBuffer = ByteBuffer.allocate(dataLength + ProtocolData.PROTOCOL_HEADER_LENGTH);
        newBuffer.put(switchRegion.get());
        // 清理原始缓冲区
        buffer.clear();
        switchRegion.set(newBuffer);
    }

}
