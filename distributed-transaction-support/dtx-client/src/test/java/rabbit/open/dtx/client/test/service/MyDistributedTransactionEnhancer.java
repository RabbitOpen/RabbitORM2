package rabbit.open.dtx.client.test.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rabbit.open.dtx.client.enhance.ext.DistributedTransactionEnhancer;

import javax.annotation.PostConstruct;

/**
 * 定制分布式事务增强器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@Component
public class MyDistributedTransactionEnhancer extends DistributedTransactionEnhancer {

    @Autowired
    SimpleTransactionManger simpleTransactionManger;

    // 注入事务管理器
    @PostConstruct
    public void init() {
        setTransactionManger(simpleTransactionManger);
    }
}
