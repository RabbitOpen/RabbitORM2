package rabbit.open.dtx.common.nio.server;

import rabbit.open.dtx.common.nio.server.ext.AbstractServerTransactionHandler;
import redis.clients.jedis.JedisCluster;

import java.util.List;

/**
 * 基于redis的事务接口实现
 * @author xiaoqianbin
 * @date 2019/12/31
 **/
public class RedisTransactionHandler extends AbstractServerTransactionHandler {

    private JedisCluster jedisCluster;

    @Override
    protected void doCommitByGroupId(Long txGroupId, String applicationName) {

    }

    @Override
    protected void doRollbackByGroupId(Long txGroupId, String applicationName) {

    }

    @Override
    public Long getNextGlobalId() {
        return jedisCluster.incr(RedisKeyNames.DTX_GLOBAL_ID.name());
    }

    @Override
    protected void persistGroupId(Long txGroupId, String applicationName, TxStatus txStatus) {

    }

    @Override
    protected void persistGroupId(Long txGroupId, TxStatus txStatus) {

    }

    @Override
    protected void persistBranchInfo(Long txGroupId, Long txBranchId, String applicationName, TxStatus txStatus) {

    }

    @Override
    public void confirmBranchCommit(String applicationName, Long txGroupId, Long txBranchId) {

    }

    @Override
    public void confirmBranchRollback(String applicationName, Long txGroupId, Long txBranchId) {

    }

    @Override
    public void lockData(String applicationName, Long txGroupId, Long txBranchId, List<String> locks) {

    }

}
