package rabbit.open.dtx.common.nio.pub.inter;

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

}
