package rabbit.open.dtx.client.test.service;

import org.springframework.stereotype.Component;
import rabbit.open.dtx.client.context.ext.AbstractTransactionManger;
import rabbit.open.dtx.client.enhance.ext.DistributedTransactionEnhancer;
import rabbit.open.dtx.client.enhance.ext.DistributedTransactionObject;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 定制分布式事务增强器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@Component
public class MyDistributedTransactionEnhancer extends DistributedTransactionEnhancer {

    // 注入事务管理器
    @PostConstruct
    public void init() {
        transactionManger = new AbstractTransactionManger() {

            AtomicLong idGenerator = new AtomicLong(0);

            @Override
            public DistributedTransactionObject getTransactionObject() {
                return new DistributedTransactionObject(idGenerator.getAndAdd(1L));
            }
        };
    }
}
