package rabbit.open.dtx.common.nio.server.handler;

import rabbit.open.dtx.common.nio.exception.DtxException;
import rabbit.open.dtx.common.nio.pub.DataHandler;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.pub.protocol.RpcProtocol;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerEventHandler;

import java.lang.reflect.InvocationTargetException;
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
        RpcProtocol protocol = (RpcProtocol) protocolData.getData();
        Object dtxService = eventHandler.getTransactionHandler();
        try {
            Method method = dtxService.getClass().getDeclaredMethod(protocol.getMethodName(), protocol.getArgTypes());
            return method.invoke(dtxService, protocol.getValues());
        } catch (NoSuchMethodException e) {
            return tryInvokeDefaultInterfaceMethod(protocol, dtxService, e);
        } catch (InvocationTargetException ex) {
            throw convertInvocationTargetException(ex);
        } catch (DtxException e) {
            throw e;
        } catch (Exception e) {
            throw new DtxException(e);
        }
    }

    /**
     * 尝试调用default方法
     * @param protocol
     * @param dtxService
     * @param e
     * @author xiaoqianbin
     * @date 2019/12/10
     **/
    private Object tryInvokeDefaultInterfaceMethod(RpcProtocol protocol, Object dtxService, NoSuchMethodException e) {
        try {
            Method method = getMethodFromInterface(protocol, dtxService, e);
            return method.invoke(dtxService, protocol.getValues());
        } catch (InvocationTargetException ex) {
            throw convertInvocationTargetException(ex);
        } catch (Exception ex) {
            throw new DtxException(ex);
        }
    }

    /**
     * 转换反射调用时异常
     * @param	ex
     * @author  xiaoqianbin
     * @date    2020/1/2
     **/
    private DtxException convertInvocationTargetException(InvocationTargetException ex) {
        Throwable cause = ex.getTargetException();
        if (cause instanceof DtxException) {
            return (DtxException) cause;
        } else {
            return new DtxException(cause);
        }
    }

    // 从接口中读取该方法
    private Method getMethodFromInterface(RpcProtocol protocol, Object dtxService, NoSuchMethodException e) {
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
        throw new DtxException(e);
    }
}
