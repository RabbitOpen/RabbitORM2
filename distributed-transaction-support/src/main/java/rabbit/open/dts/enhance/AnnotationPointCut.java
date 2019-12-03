package rabbit.open.dts.enhance;


import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.BeanCreationException;
import rabbit.open.dts.PointCutHandler;

import java.lang.annotation.Annotation;

/**
 * 切点
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
class AnnotationPointCut implements MethodInterceptor {

    private Class<? extends Annotation> targetAnnotation;

    private PointCutHandler pointCutHandler;

    public AnnotationPointCut(Class<? extends Annotation> targetAnnotation, PointCutHandler pointCutHandler) {
        assertField(targetAnnotation, "targetAnnotation can not be empty!");
        assertField(pointCutHandler, "pointCutHandler can not be empty!");
        this.targetAnnotation = targetAnnotation;
        this.pointCutHandler = pointCutHandler;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (null != invocation.getMethod().getAnnotation(targetAnnotation)) {
            return pointCutHandler.process(invocation);
        } else {
            return invocation.proceed();
        }
    }

    private void assertField(Object targetAnnotation, String msg) {
        if (null == targetAnnotation) {
            throw new BeanCreationException(msg);
        }
    }
}
