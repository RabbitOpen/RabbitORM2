package rabbit.open.dtx.common.nio.server;

import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.server.ext.TransactionContext;

/**
 * 锁等待队列
 * @author xiaoqianbin
 * @date 2019/12/23
 **/
public class LockContext {

    private TransactionContext context;

    private Long requestId;

    // 等待锁的事务分支id
    private Long txBranchId;

    private ChannelAgent agent;

    public LockContext(TransactionContext context, Long requestId, ChannelAgent agent, Long txBranchId) {
        this.context = context;
        this.requestId = requestId;
        this.agent = agent;
        this.txBranchId = txBranchId;
    }

    /**
     * 尝试唤醒等待锁的客户端
     * @param	lockId
     * @author  xiaoqianbin
     * @date    2019/12/23
     **/
    public boolean tryWakeup(String lockId) {
        return context.tryWakeupClient(lockId, txBranchId, requestId, agent);
    }
}
