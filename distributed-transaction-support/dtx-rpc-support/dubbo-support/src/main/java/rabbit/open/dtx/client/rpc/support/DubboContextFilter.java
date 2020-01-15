package rabbit.open.dtx.client.rpc.support;

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.*;
import rabbit.open.dtx.common.context.DistributedTransactionContext;
import rabbit.open.dtx.common.nio.client.DistributedTransactionObject;

/**
 * 分布式事务上下文处理
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
@Activate(group = Constants.CONSUMER)
public class DubboContextFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) {
        try {
            DistributedTransactionObject dtxObj = DistributedTransactionContext.getDistributedTransactionObject();
            if (null != dtxObj) {
                RpcContext.getContext().setAttachment(DubboTransactionManager.TRANSACTION_GROUP_ID, dtxObj.getTxGroupId().toString());
                RpcContext.getContext().setAttachment(DubboTransactionManager.TRANSACTION_ISOLATION, dtxObj.getIsolation().name());
            }
            return invoker.invoke(invocation);
        } finally {
            RpcContext.getContext().removeAttachment(DubboTransactionManager.TRANSACTION_GROUP_ID);
            RpcContext.getContext().removeAttachment(DubboTransactionManager.TRANSACTION_ISOLATION);
        }

    }
}
