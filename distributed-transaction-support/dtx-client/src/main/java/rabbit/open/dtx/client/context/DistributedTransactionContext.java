package rabbit.open.dtx.client.context;

import rabbit.open.dtx.client.enhance.DistributedTransactionObject;

/**
 * 环境上下文
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class DistributedTransactionContext {

    private DistributedTransactionContext() {

    }

    // 分布式事务信息context
    private static final ThreadLocal<DistributedTransactionObject> distributedTransactionObjectContext = new ThreadLocal<>();

    /**
     * 获取事务对象
     * @author  xiaoqianbin
     * @date    2019/12/4
     **/
    public static DistributedTransactionObject getDistributedTransactionObject() {
        return distributedTransactionObjectContext.get();
    }

    /**
     * 设置事务对象
     * @param	transactionObject
     * @author  xiaoqianbin
     * @date    2019/12/4
     **/
    public static void setDistributedTransactionObject(DistributedTransactionObject transactionObject) {
        distributedTransactionObjectContext.set(transactionObject);
    }

    /**
     * 清除事务对象
     * @author  xiaoqianbin
     * @date    2019/12/4
     **/
    public static void clear() {
        distributedTransactionObjectContext.remove();
    }

}
