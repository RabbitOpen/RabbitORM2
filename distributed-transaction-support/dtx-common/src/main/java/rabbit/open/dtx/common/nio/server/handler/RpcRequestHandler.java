package rabbit.open.dtx.common.nio.server.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import rabbit.open.dtx.common.nio.exception.RpcException;
import rabbit.open.dtx.common.nio.pub.DataHandler;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.pub.protocol.RabbitProtocol;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerEventHandler;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RPC调用数据处理器
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
class RpcRequestHandler implements DataHandler {

    AbstractServerEventHandler eventHandler;

    public RpcRequestHandler(AbstractServerEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    /***
     * 处理rpc调用
     * @param    protocolData
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    @Override
    public Object process(ProtocolData protocolData) {
        RabbitProtocol protocol = (RabbitProtocol) protocolData.getData();
        if (StringUtils.isEmpty(protocol.getNamespace())) {
            throw new RpcException("namespace can't be empty");
        }
        Object dtxService = eventHandler.getTransactionHandler();
        if (null == dtxService) {
            throw new RpcException(String.format("namespace [%s] not existed", protocol.getNamespace()));
        }
        try {
            Method method = dtxService.getClass().getDeclaredMethod(protocol.getMethodName(), protocol.getArgTypes());
            return method.invoke(dtxService, protocol.getValues());
        } catch (NoSuchMethodException e) {
            return tryInvokeDefaultInterfaceMethod(protocol, dtxService, e);
        } catch (Exception e) {
            throw new RpcException(e);
        }
    }

    /**
     * 尝试调用default方法
     * @param	protocol
	 * @param	dtxService
	 * @param	e
     * @author  xiaoqianbin
     * @date    2019/12/10
     **/
    private Object tryInvokeDefaultInterfaceMethod(RabbitProtocol protocol, Object dtxService, NoSuchMethodException e) {
        try {
            Method method = getMethodFromInterface(protocol, dtxService, e);
            return method.invoke(dtxService, protocol.getValues());
        } catch (Exception ex) {
            throw new RpcException(ex);
        }
    }

    // 从接口中读取该方法
    private Method getMethodFromInterface(RabbitProtocol protocol, Object dtxService, NoSuchMethodException e) {
        List<Class<?>> allInterfaces = new ArrayList<>();
        Class<?> clz = dtxService.getClass();
        while (!clz.equals(Object.class)) {
            allInterfaces.addAll(Arrays.asList(clz.getInterfaces()));
            clz = clz.getSuperclass();
        }
        for (Class<?> anInterface : allInterfaces) {
            try {
                return anInterface.getDeclaredMethod(protocol.getMethodName(), protocol.getArgTypes());
            } catch (NoSuchMethodException ex) {
                // TO DO: IGNORE
            }
        }
        throw new RpcException(e);
    }
}
