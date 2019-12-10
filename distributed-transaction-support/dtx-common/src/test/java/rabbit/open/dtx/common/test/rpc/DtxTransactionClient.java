package rabbit.open.dtx.common.test.rpc;

import rabbit.open.dtx.common.nio.pub.TransactionHandler;
import rabbit.open.dtx.common.spring.anno.Reference;

/**
 * rpc调用
 * @author xiaoqianbin
 * @date 2019/12/9
 **/
@Reference(transactionManager = "rpcTransactionManger", timeoutSeconds = 3)
public class DtxTransactionClient implements TransactionHandler {
}
