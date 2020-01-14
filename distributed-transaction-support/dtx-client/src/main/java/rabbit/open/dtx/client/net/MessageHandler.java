package rabbit.open.dtx.client.net;

/**
 * 事务消息处理
 * @author xiaoqianbin
 * @date 2019/12/5
 **/
public interface MessageHandler {

    /**
     * 回滚事务分支
     * @param applicationName
     * @param txGroupId
     * @param txBranchId
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    boolean rollback(String applicationName, Long txGroupId, Long txBranchId);

    /**
     * 提交事务分支
     * @author xiaoqianbin
     * @date 2019/12/5
     **/
    boolean commit();
}
