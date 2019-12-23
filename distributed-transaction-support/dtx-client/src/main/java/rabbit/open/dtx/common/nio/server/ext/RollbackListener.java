package rabbit.open.dtx.common.nio.server.ext;

import rabbit.open.dtx.common.nio.pub.ChannelAgent;

/**
 * 回滚监听器
 * @author xiaoqianbin
 * @date 2019/12/12
 **/
public class RollbackListener {

    private ChannelAgent agent;

    private Long requestId;

    public boolean rollbackCompleted() {
        if (!agent.isClosed()) {
            TransactionContext.callUnconcernedException(() -> agent.response(null, requestId));
            return true;
        }
        return false;
    }

    public RollbackListener(ChannelAgent agent, Long requestId) {
        this.agent = agent;
        this.requestId = requestId;
    }
}
