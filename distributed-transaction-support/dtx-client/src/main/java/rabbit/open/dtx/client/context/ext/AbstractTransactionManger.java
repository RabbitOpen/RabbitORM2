package rabbit.open.dtx.client.context.ext;

import rabbit.open.dtx.client.context.DistributedTransactionContext;
import rabbit.open.dtx.client.context.DistributedTransactionManger;
import rabbit.open.dtx.client.enhance.ext.DistributedTransactionObject;

/**
 * 抽象事务管理器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public abstract class AbstractTransactionManger implements DistributedTransactionManger {

    @Override
    public void beginTransaction(DistributedTransactionObject transactionObject) {
        DistributedTransactionContext.setDistributedTransactionObject(transactionObject);
    }

    @Override
    public void rollback(DistributedTransactionObject transactionObject) {
        DistributedTransactionContext.clear();
    }

    @Override
    public void commit(DistributedTransactionObject transactionObject) {
        DistributedTransactionContext.clear();
    }
}
