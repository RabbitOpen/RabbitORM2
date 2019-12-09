package rabbit.open.dts.common.spring.anno;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 声明rpc服务
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
@Inherited
public @interface DtxService {

}
