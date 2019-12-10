package rabbit.open.dtx.common.nio.server.handler;

import rabbit.open.dtx.common.nio.pub.DataHandler;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.pub.protocol.KeepAlive;

/**
 * 心跳数据处理器
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
class KeepAliveHandler implements DataHandler {

    /***
     * 心跳数据处理器
     * @param	protocolData
     * @author  xiaoqianbin
     * @date    2019/12/7
     **/
    @Override
    public Object process(ProtocolData protocolData) {
        return new KeepAlive();
    }
}
