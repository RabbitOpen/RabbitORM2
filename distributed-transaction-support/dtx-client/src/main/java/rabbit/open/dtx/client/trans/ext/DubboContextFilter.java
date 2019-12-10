package rabbit.open.dtx.client.trans.ext;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import rabbit.open.dtx.client.context.DistributedTransactionContext;
import rabbit.open.dtx.common.nio.client.DistributedTransactionObject;

/**
 * 分布式事务上下文处理
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
@Activate(group = Constants.CONSUMER)
public class DubboContextFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        try {
            DistributedTransactionObject transactionObject = DistributedTransactionContext.getDistributedTransactionObject();
            if (null != transactionObject) {
                RpcContext.getContext().setAttachment(DubboTransactionManager.TRANSACTION_GROUP_ID, transactionObject.getTxGroupId().toString());
            }
            return invoker.invoke(invocation);
        } finally {
            RpcContext.getContext().removeAttachment(DubboTransactionManager.TRANSACTION_GROUP_ID);
        }

    }
}
