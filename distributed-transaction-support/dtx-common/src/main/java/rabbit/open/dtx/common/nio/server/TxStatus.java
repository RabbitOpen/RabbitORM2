package rabbit.open.dtx.common.nio.server;

/**
 * 事务状态枚举
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
public enum TxStatus {
    OPEN("事务开启状态"),
    COMMITTING("事务正在提交"),
    COMMITTED("事务已提交"),
    COMMIT_FAILED("提交失败"),
    ROLL_BACKED("事务已回滚"),
    ROLL_BACKING("事务回滚中"),
    ROLLBACK_FAILED("回滚失败");
    TxStatus(String desc) {}
}
