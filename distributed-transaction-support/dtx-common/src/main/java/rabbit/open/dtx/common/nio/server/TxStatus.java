package rabbit.open.dtx.common.nio.server;

/**
 * 事务状态枚举
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
public enum TxStatus {
    OPEN("事务开启状态"), COMMITTED("事务已提交"),
    ROLLBACK("事务已回滚"), COMMIT_FAILED("提交失败"),
    ROLLBACK_FAILED("回滚失败");
    TxStatus(String desc) {}
}
