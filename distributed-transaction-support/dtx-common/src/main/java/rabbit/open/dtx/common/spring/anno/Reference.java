package rabbit.open.dtx.common.spring.anno;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * rpc引入声明
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
@Inherited
public @interface Reference {

    // 事务管理器的名字
    String transactionManager();

    // rpc等待超时秒数
    long timeoutSeconds() default 0L;
}
