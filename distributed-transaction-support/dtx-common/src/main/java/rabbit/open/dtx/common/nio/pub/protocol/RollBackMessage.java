package rabbit.open.dtx.common.nio.pub.protocol;

/**
 * 回滚信息
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
public class RollBackMessage extends CommitMessage {

    public RollBackMessage(String applicationName, Long txGroupId, Long txBranchId) {
        super(applicationName, txGroupId, txBranchId);
    }

    public RollBackMessage() {
    }
}
