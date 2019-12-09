package rabbit.open.dtx.common.test.rpc;

import rabbit.open.dtx.common.nio.pub.TransactionHandler;
import rabbit.open.dtx.common.spring.anno.DtxService;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 测试dtxService
 * @author xiaoqianbin
 * @date 2019/12/9
 **/
@DtxService
public class MyTransactionService implements TransactionHandler {

    AtomicLong atomicLong = new AtomicLong(0);
    @Override
    public Long getTransactionGroupId() {
        return atomicLong.getAndAdd(1);
    }
}
