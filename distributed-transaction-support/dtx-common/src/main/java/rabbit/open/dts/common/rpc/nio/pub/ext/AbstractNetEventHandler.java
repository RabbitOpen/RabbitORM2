package rabbit.open.dts.common.rpc.nio.pub.ext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dts.common.rpc.nio.exception.ClientClosedException;
import rabbit.open.dts.common.rpc.nio.exception.InvalidPackageSizeException;
import rabbit.open.dts.common.rpc.nio.pub.ChannelAgent;
import rabbit.open.dts.common.rpc.nio.pub.NetEventHandler;
import rabbit.open.dts.common.rpc.nio.pub.ProtocolData;
import rabbit.open.dts.common.rpc.nio.server.ext.DataDispatcher;
import rabbit.open.dts.common.utils.ext.KryoObjectSerializer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

/**
 * 事件处理器
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public abstract class AbstractNetEventHandler implements NetEventHandler {

    protected Logger logger = LoggerFactory.getLogger(getClass());

    // 数据接收缓冲区(默认的缓冲区只有8K, 接收大数据时会临时开辟一个大的缓冲区，用完后直接回收)
    private ThreadLocal<ByteBuffer> switchRegion = new ThreadLocal<>();


    protected abstract DataDispatcher getDispatcher();

    @Override
    public void onConnected(ChannelAgent agent) { }

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
    /**
     * 关闭通道
     * @param agent
     * @author xiaoqianbin
     * @date 2019/12/8
     **/
    protected abstract void closeChannel(ChannelAgent agent);

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
                // 再次激活，减低monitor误判死连接的几率
                agent.active();
                switchRegion.set(agent.getDataBuffer(getDefaultBufferSize()));
                readData(agent);
                agent.getSelectionKey().interestOps(SelectionKey.OP_READ);
                wakeUpSelector(agent);
            } catch (ClientClosedException e) {
                onDisconnected(agent);
                closeChannel(agent);
            } catch (Exception e) {
                onDisconnected(agent);
                closeChannel(agent);
                logger.error(e.getMessage(), e);
            } finally {
                switchRegion.remove();
            }
        });
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
     * 解析数据
     * @param agent
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    private boolean processData(ChannelAgent agent) throws IOException {
        ByteBuffer buffer = switchRegion.get();
        if (buffer.position() < ProtocolData.PROTOCOL_HEADER_LENGTH) {
            //数据头尚未读取完整
            return false;
        }
        int pos = buffer.position();
        buffer.position(0);
        int dataLength = buffer.getInt();
        if (dataLength > buffer.capacity() - ProtocolData.PROTOCOL_HEADER_LENGTH) {
            assertPackageDataLength(dataLength);
            // 如果实际需要接收的数据超过了默认buffer长度, 则生成临时缓冲用来处理数据
            switchDataCache(pos, dataLength);
            logger.warn("generate temp buffer to process request data, buffer size [{}] : ", switchRegion.get().limit());
            return false;
        }
        if (packetReadOver(pos, dataLength)) {
            byte[] data = new byte[dataLength];
            buffer.get(data, 0, dataLength);
            buffer.compact();
            buffer.position(pos - dataLength - ProtocolData.PROTOCOL_HEADER_LENGTH);
            ProtocolData protocolData = new KryoObjectSerializer().deserialize(data, ProtocolData.class);
            processData(protocolData, agent);
            return 0 == buffer.position();
        } else {
            // reset position
            buffer.position(pos);
            return false;
        }
    }

    /**
     * 处理协议数据
     * @param protocolData
     * @param agent
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    protected void processData(ProtocolData protocolData, ChannelAgent agent) throws IOException {
        try {
            agent.response(getDispatcher().process(protocolData), protocolData.getRequestId());
        } catch (Exception e) {
            agent.error(e, protocolData.getRequestId());
        }
    }

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
