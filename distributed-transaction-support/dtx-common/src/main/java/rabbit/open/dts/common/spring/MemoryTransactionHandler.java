package rabbit.open.dts.common.spring;

import rabbit.open.dts.common.rpc.nio.pub.DataType;
import rabbit.open.dts.common.rpc.nio.pub.TransactionHandler;
import rabbit.open.dts.common.spring.anno.DtxService;

/**
 * 基于内存的事务处理器
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
@DtxService()
public class MemoryTransactionHandler implements TransactionHandler {

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
        return null;
    }

    @Override
    public Long getTransactionGroupId() {
        return null;
    }

}
