package rabbit.open.dtx.client.test.impl;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface MyAop {
}
