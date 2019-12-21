package rabbit.open.dtx.common.spring.enhance;

import org.aopalliance.intercept.MethodInvocation;

import java.lang.annotation.Annotation;

/**
 * 自定义注解处理器
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
public interface PointCutHandler<T extends Annotation> {

    /**
     * 自定义注解环绕处理
     * @param    invocation
     * @param    annotation
     * @author xiaoqianbin
     * @date 2019/12/3
     **/
    Object process(MethodInvocation invocation, T annotation);

}
