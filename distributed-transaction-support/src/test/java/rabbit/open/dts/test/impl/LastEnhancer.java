package rabbit.open.dts.test.impl;

import org.springframework.stereotype.Component;
import rabbit.open.dts.PointCutHandler;
import rabbit.open.dts.enhance.AbstractAnnotationEnhancer;

import java.lang.annotation.Annotation;

/**
 * 自定义增强器
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
@Component
public class LastEnhancer extends AbstractAnnotationEnhancer {

    @Override
    protected Class<? extends Annotation> getTargetAnnotation() {
        return MyAop.class;
    }

    @Override
    protected PointCutHandler getHandler() {
        return invocation -> getClass().getSimpleName() + invocation.proceed();
    }

    @Override
    protected PointCutPosition getPointCutPosition() {
        return PointCutPosition.LAST;
    }
}
