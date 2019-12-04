package rabbit.open.dtx.client.context;

import java.io.Serializable;

/**
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public interface DistributedTransactionManger extends Serializable {

    /**
     * 获取一个事务对象
     * @author  xiaoqianbin
     * @date    2019/12/4
     **/
    DistributedTransactionObject getTransactionObject();

    /**
     * 开启事务
     * @param	transactionObject
     * @author  xiaoqianbin
     * @date    2019/12/4
     **/
    void beginTransaction(DistributedTransactionObject transactionObject);

    /**
     * 回滚事务
     * @param	transactionObject
     * @author  xiaoqianbin
     * @date    2019/12/4
     **/
    void rollback(DistributedTransactionObject transactionObject);

    /**
     * 提交事务
     * @param	transactionObject
     * @author  xiaoqianbin
     * @date    2019/12/4
     **/
    void commit(DistributedTransactionObject transactionObject);
}
