package rabbit.open.dtx.client.test.service;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rabbit.open.dtx.client.enhance.DistributedTransactionEnhancer;
import rabbit.open.dtx.common.annotation.DistributedTransaction;

import javax.annotation.PostConstruct;

/**
 * 定制分布式事务增强器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@Component
@SuppressWarnings("serial")
public class MyDistributedTransactionEnhancer extends DistributedTransactionEnhancer {

    @Autowired
    SimpleTransactionManager simpleTransactionManger;

    private boolean nestedOnly = false;

    // 注入事务管理器
    @PostConstruct
    public void init() {
        setTransactionManger(simpleTransactionManger);
        setCore(5);
        setMaxConcurrence(20);
    }

    @Override
    protected boolean isNestedOnly(MethodInvocation invocation, DistributedTransaction annotation) {
        nestedOnly = super.isNestedOnly(invocation, annotation);
        return nestedOnly;
    }

    public boolean isNestedOnly() {
        return nestedOnly;
    }

    public void setNestedOnly(boolean nestedOnly) {
        this.nestedOnly = nestedOnly;
    }
}
