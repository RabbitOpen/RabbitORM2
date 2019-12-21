package rabbit.open.dtx.common.nio.pub.protocol;

/**
 * 提交信息
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
public class CommitMessage {

    private String applicationName;

    private Long txGroupId;

    private Long txBranchId;

    public CommitMessage(String applicationName, Long txGroupId, Long txBranchId) {
        this.applicationName = applicationName;
        this.txGroupId = txGroupId;
        this.txBranchId = txBranchId;
    }

    public CommitMessage() {
    }

    public String getApplicationName() {
        return applicationName;
    }

    public Long getTxGroupId() {
        return txGroupId;
    }

    public Long getTxBranchId() {
        return txBranchId;
    }
}
