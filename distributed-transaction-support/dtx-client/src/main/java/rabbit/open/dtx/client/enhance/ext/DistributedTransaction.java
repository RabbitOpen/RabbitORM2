package rabbit.open.dtx.client.enhance.ext;

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
    long rollbackTimeoutSeconds() default 10L;
}
