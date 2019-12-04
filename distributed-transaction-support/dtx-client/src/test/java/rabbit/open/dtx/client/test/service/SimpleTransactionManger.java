package rabbit.open.dtx.client.test.service;

import org.springframework.stereotype.Component;
import rabbit.open.dtx.client.context.ext.AbstractTransactionManger;
import rabbit.open.dtx.client.enhance.ext.DistributedTransactionObject;

import java.util.concurrent.atomic.AtomicLong;

/**
 *
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@Component
public class SimpleTransactionManger extends AbstractTransactionManger {

    AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public DistributedTransactionObject newTransactionObject() {
        return new DistributedTransactionObject(idGenerator.getAndAdd(1L));
    }
}
