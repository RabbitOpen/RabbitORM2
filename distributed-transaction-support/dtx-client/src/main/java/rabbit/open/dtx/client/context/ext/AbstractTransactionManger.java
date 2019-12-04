package rabbit.open.dtx.client.context.ext;

import rabbit.open.dtx.client.context.DistributedTransactionManger;
import rabbit.open.dtx.client.context.DistributedTransactionObject;

/**
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public abstract class AbstractTransactionManger implements DistributedTransactionManger {

    @Override
    public DistributedTransactionObject getTransactionObject() {
        return null;
    }

    @Override
    public void beginTransaction(DistributedTransactionObject transactionObject) {

    }

    @Override
    public void rollback(DistributedTransactionObject transactionObject) {

    }

    @Override
    public void commit(DistributedTransactionObject transactionObject) {

    }
}
