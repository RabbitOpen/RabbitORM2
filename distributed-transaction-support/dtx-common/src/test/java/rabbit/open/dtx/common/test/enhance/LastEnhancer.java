package rabbit.open.dtx.common.test.enhance;

import org.springframework.stereotype.Component;
import rabbit.open.dtx.common.spring.enhance.AbstractAnnotationEnhancer;
import rabbit.open.dtx.common.spring.enhance.PointCutHandler;

/**
 * 自定义增强器
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
@SuppressWarnings("serial")
@Component
public class LastEnhancer extends AbstractAnnotationEnhancer<MyAop> {

    @Override
    protected PointCutHandler<MyAop> getHandler() {
        return (invocation, annotation) -> {
            try {
                return getClass().getSimpleName() + invocation.proceed();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }

    @Override
    protected PointCutPosition getPointCutPosition() {
        return PointCutPosition.LAST;
    }
}
