package rabbit.open.dtx.common.nio.server.handler;

import rabbit.open.dtx.common.exception.DtxException;
import rabbit.open.dtx.common.nio.pub.DataHandler;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.pub.protocol.RpcProtocol;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC调用数据处理器
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class RpcRequestHandler implements DataHandler {

    // 缓存接口和处理器之间的映射关系
    private Map<Class<?>, Object> handlerMap = new ConcurrentHashMap<>();

    public void registerHandler(Class<?> clz, Object handler) {
        handlerMap.put(clz, handler);
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
        Object service = null;
        try {
            service = handlerMap.get(Class.forName(protocol.getNamespace()));
            Method method = service.getClass().getDeclaredMethod(protocol.getMethodName(), protocol.getArgTypes());
            return method.invoke(service, protocol.getValues());
        } catch (NoSuchMethodException e) {
            return tryInvokeDefaultInterfaceMethod(protocol, service, e);
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
     * @param service
     * @param e
     * @author xiaoqianbin
     * @date 2019/12/10
     **/
    private Object tryInvokeDefaultInterfaceMethod(RpcProtocol protocol, Object service, NoSuchMethodException e) {
        try {
            Method method = getMethodFromInterface(protocol, service, e);
            return method.invoke(service, protocol.getValues());
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
    private Method getMethodFromInterface(RpcProtocol protocol, Object service, NoSuchMethodException e) {
        List<Class<?>> allInterfaces = new ArrayList<>();
        Class<?> clz = service.getClass();
        while (!clz.equals(Object.class)) {
            allInterfaces.addAll(Arrays.asList(clz.getInterfaces()));
            clz = clz.getSuperclass();
        }
        for (Class<?> interfaceClz : allInterfaces) {
            try {
                return interfaceClz.getDeclaredMethod(protocol.getMethodName(), protocol.getArgTypes());
            } catch (NoSuchMethodException ex) {
                // TO DO: IGNORE
            }
        }
        throw new DtxException(e);
    }
}
