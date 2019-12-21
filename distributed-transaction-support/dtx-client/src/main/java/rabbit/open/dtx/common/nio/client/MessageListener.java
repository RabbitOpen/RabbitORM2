package rabbit.open.dtx.common.nio.client;

/**
 * 消息处理器
 * @author xiaoqianbin
 * @date 2019/12/16
 **/
public interface MessageListener {
    void onMessageReceived(Object msg);
}
