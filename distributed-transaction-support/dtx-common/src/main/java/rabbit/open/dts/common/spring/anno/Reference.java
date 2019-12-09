package rabbit.open.dts.common.spring.anno;

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

    // 注册中心配置对象的spring bean名字
    String registryBeanName();
}
