package rabbit.open.dtx.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 分布式事务注解
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface DistributedTransaction {

    // 任务超时时间, 默认永不超时
    long transactionTimeoutSeconds() default Long.MAX_VALUE;

    // 回滚超时时间
    long rollbackTimeoutSeconds() default 3L;

    // 事务传播特性，默认没有事务就直接开启事务
    Propagation propagation() default Propagation.REQUIRED;

    // 默认不加锁
    Isolation isolation() default Isolation.UNLOCK;

    // 回滚策略
    RollbackPolicy rollback() default RollbackPolicy.STRICT;
}
