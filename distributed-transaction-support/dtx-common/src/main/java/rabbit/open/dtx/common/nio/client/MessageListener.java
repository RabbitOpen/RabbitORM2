package rabbit.open.dtx.common.nio.client;

/**
 * 消息监听接口
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
@FunctionalInterface
public interface MessageListener {

    void onMessageReceived(Object msg);
}
