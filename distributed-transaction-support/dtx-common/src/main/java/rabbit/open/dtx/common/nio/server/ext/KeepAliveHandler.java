package rabbit.open.dtx.common.nio.server.ext;

import rabbit.open.dtx.common.nio.pub.DataHandler;
import rabbit.open.dtx.common.nio.pub.KeepAlive;
import rabbit.open.dtx.common.nio.pub.ProtocolData;

import java.io.Serializable;

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
    public Serializable process(ProtocolData protocolData) {
        return new KeepAlive();
    }
}
