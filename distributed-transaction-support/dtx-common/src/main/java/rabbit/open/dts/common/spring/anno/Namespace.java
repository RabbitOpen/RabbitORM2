package rabbit.open.dts.common.spring.anno;

import java.lang.annotation.*;

/**
 * 声明rpc服务命名空间
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface Namespace {

    // 命名空间的名字
    String value();
}
