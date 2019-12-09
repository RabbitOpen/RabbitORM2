package rabbit.open.dtx.common.nio.pub;

/**
 * 网络事件接口
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public interface NetEventHandler {

    /**
     * 客户端连接事件
     * @param	agent
     * @author  xiaoqianbin
     * @date    2019/12/7
     **/
    void onConnected(ChannelAgent agent);

    /**
     * 客户端断开事件
     * @param	agent
     * @author  xiaoqianbin
     * @date    2019/12/7
     **/
    void onDisconnected(ChannelAgent agent);

    /**
     * 数据接收事件
     * @param	agent
     * @author  xiaoqianbin
     * @date    2019/12/7
     **/
    void onDataReceived(ChannelAgent agent);

}
