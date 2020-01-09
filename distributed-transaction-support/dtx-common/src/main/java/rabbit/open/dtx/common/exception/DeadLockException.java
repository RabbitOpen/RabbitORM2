package rabbit.open.dtx.common.exception;

/**
 * DTX死锁异常， 不同的分支尝试获取同一个资源
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@SuppressWarnings("serial")
public class DeadLockException extends DtxException {

    public DeadLockException(Long lockBranch, Long currentBranch, String lock) {
        super(String.format("dead lock is detected! branch[%s] tried to hold lock[%s] which was hold by branch[%s]",
                currentBranch, lock, lockBranch));
    }
}
