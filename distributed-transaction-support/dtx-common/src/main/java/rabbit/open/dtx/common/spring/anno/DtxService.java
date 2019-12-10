package rabbit.open.dtx.common.spring.anno;

import org.springframework.stereotype.Service;

import java.lang.annotation.*;

/**
 * 声明rpc服务
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Service
@Inherited
public @interface DtxService {

}
