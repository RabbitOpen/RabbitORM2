package rabbit.open.dtx.common.context;

import rabbit.open.dtx.common.nio.client.DistributedTransactionObject;

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

    // 回滚超时时间
    private static final ThreadLocal<Long> rollbackTimeoutContext = new ThreadLocal<>();

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
     * 设置回滚超时
     * @param	timeoutSeconds
     * @author  xiaoqianbin
     * @date    2019/12/11
     **/
    public static void setRollbackTimeout(long timeoutSeconds) {
        rollbackTimeoutContext.set(timeoutSeconds);
    }

    /**
     * 获取回滚超时设置
     * @author  xiaoqianbin
     * @date    2019/12/11
     **/
    public static Long getRollbackTimeout() {
        return rollbackTimeoutContext.get();
    }

    /**
     * 清除事务对象
     * @author  xiaoqianbin
     * @date    2019/12/4
     **/
    public static void clear() {
        distributedTransactionObjectContext.remove();
        rollbackTimeoutContext.remove();
    }

}
