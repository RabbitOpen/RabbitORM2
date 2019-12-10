package rabbit.open.dtx.common.nio.server;

/**
 * 事务状态枚举
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
public enum TxStatus {
    OPEN, COMMIT, ROLLBACK, COMMIT_FAILED, ROLLBACK_FAILED;
}
