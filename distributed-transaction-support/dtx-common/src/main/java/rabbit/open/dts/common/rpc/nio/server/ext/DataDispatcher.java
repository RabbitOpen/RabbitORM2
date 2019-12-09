package rabbit.open.dts.common.rpc.nio.server.ext;

import rabbit.open.dts.common.rpc.nio.exception.RpcException;
import rabbit.open.dts.common.rpc.nio.pub.DataHandler;
import rabbit.open.dts.common.rpc.nio.pub.KeepAlive;
import rabbit.open.dts.common.rpc.nio.pub.ProtocolData;
import rabbit.open.dts.common.rpc.nio.pub.RabbitProtocol;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 数据处理分发器
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class DataDispatcher implements DataHandler {

    private Map<Class<?>, DataHandler> handlerMap = new ConcurrentHashMap<>();

    public DataDispatcher() {
        handlerMap.put(RabbitProtocol.class, new RpcRequestHandler());
        handlerMap.put(KeepAlive.class, new KeepAliveHandler());
    }

    AtomicLong received = new AtomicLong(0);
    @Override
    public Serializable process(ProtocolData protocolData) {
        if (null == protocolData.getData()) {
            throw new RpcException("protocol data can't be empty");
        }
        DataHandler handler = handlerMap.get(protocolData.getData().getClass());
        if (null == handler) {
            throw new RpcException(String.format("unknown data type {%s}", protocolData.getData().getClass()));
        }
        return handler.process(protocolData);
    }

}
