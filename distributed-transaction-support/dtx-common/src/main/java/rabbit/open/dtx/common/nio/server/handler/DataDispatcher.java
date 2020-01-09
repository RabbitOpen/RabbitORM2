package rabbit.open.dtx.common.nio.server.handler;

import rabbit.open.dtx.common.exception.DtxException;
import rabbit.open.dtx.common.nio.pub.DataHandler;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.pub.inter.ProtocolHandler;
import rabbit.open.dtx.common.nio.pub.protocol.Application;
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

    private RpcRequestHandler requestHandler = new RpcRequestHandler();

    public DataDispatcher() {
        this(null);
    }

    public DataDispatcher(AbstractServerEventHandler eventHandler) {
        ApplicationProtocolHandler handler = new ApplicationProtocolHandler(eventHandler);
        handlerMap.put(RpcProtocol.class, requestHandler);
        handlerMap.put(Application.class, handler);
        registerInterfaceHandler(ProtocolHandler.class, handler);
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
     * @param    handler
     * @author xiaoqianbin
     * @date 2019/12/31
     **/
    public void registerHandler(Class<?> dataClz, DataHandler handler) {
        handlerMap.put(dataClz, handler);
    }

    /**
     * 注册接口处理器
     * @param    interfaceClz
     * @param    handler
     * @author xiaoqianbin
     * @date 2020/1/9
     **/
    public void registerInterfaceHandler(Class<?> interfaceClz, Object handler) {
        requestHandler.registerHandler(interfaceClz, handler);
    }


}
