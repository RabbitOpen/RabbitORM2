package rabbit.open.dtx.common.nio.server;

import rabbit.open.dtx.common.nio.exception.DistributedTransactionException;
import rabbit.open.dtx.common.nio.pub.CallHelper;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.protocol.CommitMessage;
import rabbit.open.dtx.common.nio.pub.protocol.RollBackMessage;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerEventHandler;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerTransactionHandler;
import rabbit.open.dtx.common.nio.server.handler.ApplicationDataHandler;

import javax.annotation.PreDestroy;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <b>基于redis的事务接口实现， 事务context 采用redis map结构存储, 结构详细信息如下
 * groupInfo:          app|status|groupId
 * branchInfo:         branchApp|status|branchId
 * rollbackContext:    requestId|clientInstanceId
 * </b>
 * @author xiaoqianbin
 * @date 2019/12/31
 **/
public class RedisTransactionHandler extends AbstractServerTransactionHandler {

    private JedisClient jedisClient;

    private Sweeper sweeper;

    public RedisTransactionHandler() {
        sweeper = new Sweeper(this);
        sweeper.start();
    }

    public Sweeper getSweeper() {
        return sweeper;
    }

    /**
     * 提交事务
     * @param txGroupId
     * @param applicationName
     * @author xiaoqianbin
     * @date 2020/1/1
     **/
    @Override
    protected void doCommitByGroupId(Long txGroupId, String applicationName) {
        Map<String, String> context = jedisClient.hgetAll(getGroupIdKey(txGroupId));
        if (!existUnConfirmedBranch(context)) {
            removeTransactionContext(txGroupId);
            return;
        }
        for (Map.Entry<String, String> entry : context.entrySet()) {
            if (entry.getKey().startsWith(RedisKeyNames.BRANCH_INFO.name())) {
                String[] info = entry.getValue().split("\\|");
                dispatchCommitInfo(txGroupId, info[0], Long.parseLong(info[2]));
            }
        }
    }

    /**
     * 清理30分钟还未结束的context
     * @author  xiaoqianbin
     * @date    2020/1/2
     **/
    public void clearDeadContext() {
        if (null == jedisClient) {
            return;
        }
        Set<String> list = jedisClient.zrangeByScore(RedisKeyNames.DTX_CONTEXT_LIST.name(), 0,
                System.currentTimeMillis() - 30d * 60 * 1000);
        for (String key : list) {
            Map<String, String> context = jedisClient.hgetAll(key);
            if (shouldRollback(context)) {
                String groupInfo = context.get(RedisKeyNames.GROUP_INFO.name());
                long txGroupId = Long.parseLong(groupInfo.split("\\|")[2]);
                if (rollbackByContext(context, txGroupId)) {
                    logger.warn("dead context[{}] is cleared", context);
                }
            } else {
                jedisClient.zrem(RedisKeyNames.DTX_CONTEXT_LIST.name(), key);
                jedisClient.del(key);
                logger.warn("dead context[{}] is cleared", context);
            }
        }
    }

    /**
     * 根据context信息和 groupId进行回滚
     * @param	context
	 * @param	txGroupId
     * @author  xiaoqianbin
     * @date    2020/1/2
     **/
    private boolean rollbackByContext(Map<String, String> context, long txGroupId) {
        boolean result = true;
        for (Map.Entry<String, String> entry : context.entrySet()) {
            if (entry.getKey().startsWith(RedisKeyNames.BRANCH_INFO.name())) {
                String[] info = entry.getValue().split("\\|");
                result = result && dispatchRollbackInfo(txGroupId, info[0], Long.parseLong(info[2]));
            }
        }
        return result;
    }

    /**
     * 下发分支提交信息
     * @param txGroupId
     * @param app
     * @param txBranchId
     * @author xiaoqianbin
     * @date 2020/1/1
     **/
    private void dispatchCommitInfo(Long txGroupId, String app, long txBranchId) {
        List<ChannelAgent> agents = ApplicationDataHandler.getAgents(app);
        for (ChannelAgent agent : agents) {
            if (!agent.isClosed()) {
                try {
                    agent.notify(new CommitMessage(app, txGroupId, txBranchId));
                    logger.debug("dispatch commit message ({} --> {}) ", txGroupId, txBranchId);
                    break;
                } catch (Exception e) {
                    // TO DO: 忽略节点挂了的异常
                }
            }
        }
    }

    @Override
    protected void doRollbackByGroupId(Long txGroupId, String applicationName) {
        logger.debug("{} doRollback txGroupId: {}", applicationName, txGroupId);
        Map<String, String> context = jedisClient.hgetAll(getGroupIdKey(txGroupId));
        if (shouldRollback(context)) {
            // 如果需要回滚才暂时挂起客户端
            AbstractServerEventHandler.suspendRequest();
            saveRollbackContext(txGroupId);
            rollbackByContext(context, txGroupId);
        } else {
            removeTransactionContext(txGroupId);
        }
    }

    // 保存回滚上下文信息
    private void saveRollbackContext(Long txGroupId) {
        jedisClient.hset(getGroupIdKey(txGroupId), RedisKeyNames.ROLLBACK_CONTEXT.name(), AbstractServerEventHandler.getCurrentRequestId()
                + "|" + AbstractServerEventHandler.getCurrentAgent().getClientInstanceId());
    }

    /**
     * 判断是否需要回滚分支（如果没有任何一个分支提交了就不需要回滚）
     * @param context
     * @author xiaoqianbin
     * @date 2020/1/1
     **/
    private boolean shouldRollback(Map<String, String> context) {
        for (Map.Entry<String, String> entry : context.entrySet()) {
            if (entry.getKey().startsWith(RedisKeyNames.BRANCH_INFO.name())) {
                String[] info = entry.getValue().split("\\|");
                if (TxStatus.COMMITTED.name().equals(info[1])) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean dispatchRollbackInfo(Long txGroupId, String app, long txBranchId) {
        List<ChannelAgent> agents = ApplicationDataHandler.getAgents(app);
        for (ChannelAgent agent : agents) {
            if (!agent.isClosed()) {
                try {
                    agent.notify(new RollBackMessage(app, txGroupId, txBranchId));
                    logger.debug("dispatch rollback message ({} --> {}) ", txGroupId, txBranchId);
                    return true;
                } catch (Exception e) {
                    // TO DO: 忽略节点挂了的异常
                }
            }
        }
        return false;
    }

    /**
     * 全局id生成器
     * @author xiaoqianbin
     * @date 2020/1/1
     **/
    @Override
    public Long getNextGlobalId() {
        return jedisClient.incr(RedisKeyNames.DTX_GLOBAL_ID.name());
    }

    /**
     * 持久化分组id
     * @param txGroupId
     * @param applicationName
     * @author xiaoqianbin
     * @date 2020/1/1
     **/
    @Override
    protected void openTransaction(Long txGroupId, String applicationName) {
        this.persistGroupStatus(txGroupId, applicationName, TxStatus.OPEN);
        addContextIndex(txGroupId);
    }

    // 记录当前context信息
    private void addContextIndex(Long txGroupId) {
        jedisClient.zadd(RedisKeyNames.DTX_CONTEXT_LIST.name(), System.currentTimeMillis(), getGroupIdKey(txGroupId));
    }

    /**
     * 持久化分组id
     * @param txGroupId
     * @param applicationName
     * @param txStatus
     * @author xiaoqianbin
     * @date 2020/1/1
     **/
    @Override
    protected void persistGroupStatus(Long txGroupId, String applicationName, TxStatus txStatus) {
        if (TxStatus.COMMITTED == txStatus || TxStatus.ROLL_BACKED == txStatus) {
            Map<String, String> context = jedisClient.hgetAll(getGroupIdKey(txGroupId));
            if (context.isEmpty()) {
                throw new DistributedTransactionException(String.format("transaction %s is not existed", txGroupId));
            }
            String app = context.get(RedisKeyNames.GROUP_INFO.name()).split("\\|")[0];
            if (!applicationName.equals(app)) {
                String errMsg = String.format("txGroupId(%s) is created by '%s', but %s by '%s'", txGroupId,
                        app, TxStatus.COMMITTED == txStatus ? "committed" : "rollback", applicationName);
                logger.error(errMsg);
                throw new DistributedTransactionException(errMsg);
            }
        }
        jedisClient.hset(getGroupIdKey(txGroupId), RedisKeyNames.GROUP_INFO.name(),
                applicationName + "|" + txStatus.name() + "|" + txGroupId);
    }

    @Override
    protected void persistBranchInfo(Long txGroupId, Long txBranchId, String applicationName, TxStatus txStatus) {
        jedisClient.hset(getGroupIdKey(txGroupId), getBranchInfoKey(txBranchId),
                applicationName + "|" + txStatus.name() + "|" + txBranchId);
    }

    /**
     * 获取分支信息key
     * @param txBranchId
     * @author xiaoqianbin
     * @date 2020/1/1
     **/
    private String getBranchInfoKey(Long txBranchId) {
        return RedisKeyNames.BRANCH_INFO.name() + txBranchId.toString();
    }

    @Override
    public void confirmBranchCommit(String applicationName, Long txGroupId, Long txBranchId) {
        jedisClient.hdel(getGroupIdKey(txGroupId), getBranchInfoKey(txBranchId));
        logger.debug("{} confirmBranchCommit [{} --> {}] ", applicationName, txGroupId, txBranchId);
        Map<String, String> context = jedisClient.hgetAll(getGroupIdKey(txGroupId));
        if (!existUnConfirmedBranch(context)) {
            logger.debug("tx [{}] commit success", txGroupId);
            removeTransactionContext(txGroupId);
        }
    }

    /**
     * 唤醒之前被挂起的客户端
     * @param clientInstanceId
     * @param requestId
     * @param applicationName
     * @author xiaoqianbin
     * @date 2020/1/1
     **/
    private void wakeUpClient(Long clientInstanceId, Long requestId, String applicationName) {
        List<ChannelAgent> agents = ApplicationDataHandler.getAgents(applicationName);
        for (ChannelAgent agent : agents) {
            if (agent.getClientInstanceId().equals(clientInstanceId)) {
                CallHelper.ignoreExceptionCall(() -> agent.ack(requestId));
                return;
            }
        }
    }

    /**
     * 删除context对象
     * @param txGroupId
     * @author xiaoqianbin
     * @date 2020/1/1
     **/
    private void removeTransactionContext(Long txGroupId) {
        jedisClient.del(getGroupIdKey(txGroupId));
        // 删除context的索引信息
        jedisClient.zrem(RedisKeyNames.DTX_CONTEXT_LIST.name(), getGroupIdKey(txGroupId));
    }

    /**
     * 判断还有没有未反馈的分支
     * @param context
     * @author xiaoqianbin
     * @date 2020/1/1
     **/
    private boolean existUnConfirmedBranch(Map<String, String> context) {
        for (Map.Entry<String, String> entry : context.entrySet()) {
            if (entry.getKey().startsWith(RedisKeyNames.BRANCH_INFO.name())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void confirmBranchRollback(String applicationName, Long txGroupId, Long txBranchId) {
        jedisClient.hdel(getGroupIdKey(txGroupId), getBranchInfoKey(txBranchId));
        logger.debug("{} confirmBranchRollback [{} --> {}] ", applicationName, txGroupId, txBranchId);
        Map<String, String> context = jedisClient.hgetAll(getGroupIdKey(txGroupId));
        if (!existUnConfirmedBranch(context)) {
            logger.debug("tx [{}] rollback completed", txGroupId);
            String rollbackInfo = context.get(RedisKeyNames.ROLLBACK_CONTEXT.name());
            if (null != rollbackInfo) {
                String[] rollbackContext = rollbackInfo.split("\\|");
                wakeUpClient(Long.parseLong(rollbackContext[1]), Long.parseLong(rollbackContext[0]), applicationName);
            }
            removeTransactionContext(txGroupId);
        }
    }

    @Override
    public void lockData(String applicationName, Long txGroupId, Long txBranchId, List<String> locks) {
        // to do : ignore
    }

    @Override
    public void doRollback(Long txGroupId, String applicationName) {
        persistGroupStatus(txGroupId, applicationName, TxStatus.ROLL_BACKED);
        doRollbackByGroupId(txGroupId, applicationName);
    }

    private String getGroupIdKey(Long txGroupId) {
        return RedisKeyNames.DTX_GROUP_ID + "_" + txGroupId.toString();
    }

    public void setJedisClient(JedisClient jedisClient) {
        this.jedisClient = jedisClient;
    }

    @PreDestroy
    public void destroy() {
        sweeper.shutdown();
        jedisClient.close();
        logger.info("jedis client is closed");
    }
}
