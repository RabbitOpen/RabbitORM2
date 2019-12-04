package rabbit.open.dtx.client.enhance.ext;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@Transactional(propagation = Propagation.REQUIRED)
public @interface DistributedTransaction {

    // 任务超时时间, 默认永不超时
    long timeoutSeconds() default Long.MAX_VALUE;
}
