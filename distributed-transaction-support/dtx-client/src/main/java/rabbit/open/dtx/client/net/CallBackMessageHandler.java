package rabbit.open.dtx.client.net;

/**
 * 事务消息回调处理类
 * @author xiaoqianbin
 * @date 2019/12/5
 **/
public class CallBackMessageHandler implements MessageHandler {

    @Override
    public void rollback(String applicationName, Long txGroupId, Long txBranchId) {

    }

    @Override
    public void commit(String applicationName, Long txGroupId, Long txBranchId) {

    }
}
