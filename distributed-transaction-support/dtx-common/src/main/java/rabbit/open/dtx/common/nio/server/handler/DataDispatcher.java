package rabbit.open.dtx.common.nio.server.handler;

import rabbit.open.dtx.common.exception.DtxException;
import rabbit.open.dtx.common.nio.pub.DataHandler;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.pub.protocol.Application;
import rabbit.open.dtx.common.nio.pub.protocol.ClientInstance;
import rabbit.open.dtx.common.nio.pub.protocol.KeepAlive;
import rabbit.open.dtx.common.nio.pub.protocol.RpcProtocol;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerEventHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 数据处理分发器
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class DataDispatcher implements DataHandler {

    private Map<Class<?>, DataHandler> handlerMap = new ConcurrentHashMap<>();

    private AbstractServerEventHandler eventHandler = null;

    public DataDispatcher() {
        this(null);
    }

    public DataDispatcher(AbstractServerEventHandler eventHandler) {
        this.eventHandler = eventHandler;
        handlerMap.put(RpcProtocol.class, new RpcRequestHandler(this.eventHandler));
        handlerMap.put(KeepAlive.class, new KeepAliveHandler());
        handlerMap.put(Application.class, new ApplicationDataHandler());
        handlerMap.put(ClientInstance.class, new InstanceIDGenerator(eventHandler));
    }

    @Override
    public Object process(ProtocolData protocolData) {
        DataHandler handler = handlerMap.get(protocolData.getData().getClass());
        if (null == handler) {
            throw new DtxException(String.format("unknown data type {%s}", protocolData.getData().getClass()));
        }
        return handler.process(protocolData);
    }

    /**
     * 注册数据处理器
	 * @param	handler
     * @author  xiaoqianbin
     * @date    2019/12/31
     **/
    public void registerHandler(Class<?> clz, DataHandler handler) {
        handlerMap.put(clz, handler);
    }
}
