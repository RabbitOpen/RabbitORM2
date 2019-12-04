package rabbit.open.dts.test.impl;

import org.springframework.stereotype.Component;
import rabbit.open.dtx.client.enhance.AbstractAnnotationEnhancer;
import rabbit.open.dtx.client.enhance.PointCutHandler;

/**
 * 自定义增强器
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
@Component
public class LastEnhancer extends AbstractAnnotationEnhancer<MyAop> {

    @Override
    protected Class<MyAop> getTargetAnnotation() {
        return MyAop.class;
    }

    @Override
    protected PointCutHandler<MyAop> getHandler() {
        return (invocation, annotation) -> getClass().getSimpleName() + invocation.proceed();
    }

    @Override
    protected PointCutPosition getPointCutPosition() {
        return PointCutPosition.LAST;
    }
}
