package rabbit.open.dts;

import org.aopalliance.intercept.MethodInvocation;

/**
 * 自定义注解处理器
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
public interface PointCutHandler {

    /**
     * 自定义注解环绕处理
     * @param	invocation
     * @author  xiaoqianbin
     * @date    2019/12/3
     **/
    Object process(MethodInvocation invocation) throws Throwable;

}
