package rabbit.open.dtx.common.test.enhance;

import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

/**
 * @author xiaoqianbin
 * @date 2019/12/12
 **/
@Aspect
@Component
public class MyAspect {


    @Before("execution(* rabbit.open.dtx.common.test.enhance.HelloService.*(..))")
    public void before() {

    }
}
