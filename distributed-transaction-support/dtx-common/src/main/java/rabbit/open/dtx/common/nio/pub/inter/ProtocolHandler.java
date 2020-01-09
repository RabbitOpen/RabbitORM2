package rabbit.open.dtx.common.nio.pub.inter;

import rabbit.open.dtx.common.nio.pub.protocol.ClientInstance;

/**
 * 基础协议接口
 * @author xiaoqianbin
 * @date 2020/1/9
 **/
public interface ProtocolHandler {

    /**
     * 发送心跳数据
     * @author  xiaoqianbin
     * @date    2020/1/9
     **/
    void keepAlive();


    /**
     * 获取客户端应用的实例信息
     * @author  xiaoqianbin
     * @date    2020/1/9
     **/
    ClientInstance getClientInstanceInfo();

}
