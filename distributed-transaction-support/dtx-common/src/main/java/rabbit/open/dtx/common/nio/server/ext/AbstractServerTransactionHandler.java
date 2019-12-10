package rabbit.open.dtx.common.nio.server.ext;

import org.springframework.beans.factory.annotation.Autowired;
import rabbit.open.dtx.common.nio.pub.TransactionHandler;

/**
 * 服务端事务处理器
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public abstract class AbstractServerTransactionHandler implements TransactionHandler {

    @Autowired
    DtxServerEventHandler handler;

    @Override
    public void doBranchCommit(Long txGroupId, Long txBranchId, String applicationName) {

    }

    @Override
    public void doCommit(Long txGroupId, Long txBranchId) {

    }

    @Override
    public void doRollback(Long txGroupId) {

    }

    @Override
    public Long getTransactionBranchId(Long txGroupId, String applicationName) {
        Long txBranchId = getNextId();
        persistBranchInfo(txGroupId, txBranchId, applicationName);
        return txBranchId;
    }

    @Override
    public Long getTransactionGroupId() {
        Long txGroupId = getNextId();
        persistGroupId(txGroupId);
        return txGroupId;
    }

    /**
     * 生成一个全局唯一的id
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    protected abstract Long getNextId();

    /**
     * 持久化分组id信息
     * @param	txGroupId
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    protected abstract void persistGroupId(Long txGroupId);

    /**
     * 持久化分支id
     * @param	txGroupId
	 * @param	txBranchId
	 * @param	applicationName
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    protected abstract void persistBranchInfo(Long txGroupId, Long txBranchId, String applicationName);
}
