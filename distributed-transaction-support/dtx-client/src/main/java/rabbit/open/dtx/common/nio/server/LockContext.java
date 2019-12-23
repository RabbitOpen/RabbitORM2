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

    private ChannelAgent agent;

    public LockContext(TransactionContext context, Long requestId, ChannelAgent agent) {
        this.context = context;
        this.requestId = requestId;
        this.agent = agent;
    }

    /**
     * 获得锁
     * @param	lockId
     * @author  xiaoqianbin
     * @date    2019/12/23
     **/
    public void obtainLock(String lockId) {
        context.getLockIdWaitingQueue().remove(lockId);
        context.addHoldLock(lockId);
        if (context.getLockIdWaitingQueue().isEmpty()) {
            // 如果获取到所有的锁资源就通知客户端
            TransactionContext.callUnconcernedException(() -> agent.response(null, requestId));
        }
    }
}
