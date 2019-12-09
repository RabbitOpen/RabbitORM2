package rabbit.open.dtx.common.spring.enhance.ext;

import org.aopalliance.intercept.MethodInvocation;
import rabbit.open.dtx.common.spring.enhance.DistributedTransactionPointCut;
import rabbit.open.dtx.common.spring.enhance.PointCutHandler;

import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Reference 增强
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class ReferencePointCut<T extends Annotation> extends DistributedTransactionPointCut<T> {

    private Map<Class<?>, T> annotationCache = new ConcurrentHashMap<>();

    public ReferencePointCut(Class<T> targetAnnotation, PointCutHandler<T> pointCutHandler) {
        super(targetAnnotation, pointCutHandler);
        assertField(pointCutHandler, "pointCutHandler can not be empty!");
    }

    // 重写基类调用逻辑，直接使用代理方法调用
    @Override
    public Object invoke(MethodInvocation invocation) {
        Class<?> targetClz = invocation.getThis().getClass();
        if (!annotationCache.containsKey(targetClz)) {
            // 缓存代理注解对象
            annotationCache.put(targetClz, GenericAnnotationProxy.proxy(targetClz.getAnnotation(targetAnnotation), targetAnnotation));
        }
        return pointCutHandler.process(invocation, annotationCache.get(targetClz));
    }

}
