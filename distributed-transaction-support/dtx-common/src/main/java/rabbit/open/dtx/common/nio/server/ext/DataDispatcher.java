package rabbit.open.dtx.common.nio.server.ext;

import rabbit.open.dtx.common.nio.exception.RpcException;
import rabbit.open.dtx.common.nio.pub.DataHandler;
import rabbit.open.dtx.common.nio.pub.KeepAlive;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.pub.RabbitProtocol;

import java.io.Serializable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
