package rabbit.open.dtx.client.context;

/**
 * 环境上下文
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class DistributedTransactionContext {

    private DistributedTransactionContext() {

    }

    public static final ThreadLocal<DistributedTransactionObject> rollBackContext = new ThreadLocal<>();

    public static DistributedTransactionObject getTransactionContext() {
        return rollBackContext.get();
    }

}
