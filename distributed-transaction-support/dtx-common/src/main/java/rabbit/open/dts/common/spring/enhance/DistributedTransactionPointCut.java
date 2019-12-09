package rabbit.open.dts.common.spring.enhance;


import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.BeanCreationException;

import java.lang.annotation.Annotation;

/**
 * DistributedTransaction切点
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
public class DistributedTransactionPointCut<T extends Annotation> implements MethodInterceptor {

    protected Class<T> targetAnnotation;

    protected PointCutHandler<T> pointCutHandler;

    public DistributedTransactionPointCut(Class<T> targetAnnotation, PointCutHandler<T> pointCutHandler) {
        assertField(targetAnnotation, "targetAnnotation can not be empty!");
        this.targetAnnotation = targetAnnotation;
        this.pointCutHandler = pointCutHandler;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        T annotation = invocation.getMethod().getAnnotation(targetAnnotation);
        if (null != annotation && null != pointCutHandler) {
            return pointCutHandler.process(invocation, annotation);
        } else {
            return invocation.proceed();
        }
    }

    protected void assertField(Object targetAnnotation, String msg) {
        if (null == targetAnnotation) {
            throw new BeanCreationException(msg);
        }
    }
}
