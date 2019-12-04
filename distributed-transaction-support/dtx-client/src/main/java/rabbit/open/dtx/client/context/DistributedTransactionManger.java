package rabbit.open.dtx.client.context;

import rabbit.open.dtx.client.enhance.ext.DistributedTransactionObject;

import java.io.Serializable;

/**
 * 分布式事务管理器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public interface DistributedTransactionManger extends Serializable {

    /**
     * 新建一个事务对象
     * @author  xiaoqianbin
     * @date    2019/12/4
     **/
    DistributedTransactionObject newTransactionObject();

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
