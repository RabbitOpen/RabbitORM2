package rabbit.open.dtx.client.enhance;


import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.BeanCreationException;

import java.lang.annotation.Annotation;

/**
 * 切点
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
class AnnotationPointCut<T extends Annotation> implements MethodInterceptor {

    private Class<T> targetAnnotation;

    private PointCutHandler<T> pointCutHandler;

    public AnnotationPointCut(Class<T> targetAnnotation, PointCutHandler<T> pointCutHandler) {
        assertField(targetAnnotation, "targetAnnotation can not be empty!");
        assertField(pointCutHandler, "pointCutHandler can not be empty!");
        this.targetAnnotation = targetAnnotation;
        this.pointCutHandler = pointCutHandler;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        T annotation = invocation.getMethod().getAnnotation(targetAnnotation);
        if (null != annotation) {
            return pointCutHandler.process(invocation, annotation);
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
