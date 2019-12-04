package rabbit.open.dts.test.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rabbit.open.dtx.client.enhance.AbstractAnnotationEnhancer;
import rabbit.open.dtx.client.enhance.PointCutHandler;

/**
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
@Component
public class DBEnhancer extends AbstractAnnotationEnhancer<Transactional> {

    @Override
    protected Class<Transactional> getTargetAnnotation() {
        return Transactional.class;
    }

    @Override
    protected PointCutHandler<Transactional> getHandler() {
        return (invocation, annotation) -> {
            if (invocation.getThis() instanceof EnterpriseService) {
                EnterpriseService es = (EnterpriseService) invocation.getThis();
                es.increase();
            }
            return invocation.proceed();
        };
    }
}
