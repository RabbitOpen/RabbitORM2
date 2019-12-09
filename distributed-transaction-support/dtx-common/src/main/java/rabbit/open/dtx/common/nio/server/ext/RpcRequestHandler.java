package rabbit.open.dtx.common.nio.server.ext;

import org.springframework.util.StringUtils;
import rabbit.open.dtx.common.nio.exception.RpcException;
import rabbit.open.dtx.common.nio.pub.DataHandler;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.pub.RabbitProtocol;
import rabbit.open.dtx.common.spring.enhance.ext.DtxServiceScanner;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * RPC调用数据处理器
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
class RpcRequestHandler implements DataHandler {

    /***
     * 处理rpc调用
     * @param	protocolData
     * @author  xiaoqianbin
     * @date    2019/12/7
     **/
    @Override
    public Serializable process(ProtocolData protocolData) {
        RabbitProtocol protocol = (RabbitProtocol) protocolData.getData();
        if (StringUtils.isEmpty(protocol.getNamespace())) {
            throw new RpcException("namespace can't be empty");
        }
        Object dtxService = DtxServiceScanner.getDtxService(protocol.getNamespace());
        if (null == dtxService) {
            throw new RpcException(String.format("namespace [%s] not existed", protocol.getNamespace()));
        }
        try {
            Method method = dtxService.getClass().getDeclaredMethod(protocol.getMethodName(), protocol.getArgTypes());
            return (Serializable) method.invoke(dtxService, protocol.getValues());
        } catch (Exception e) {
            throw new RpcException(e.getMessage());
        }
    }
}
