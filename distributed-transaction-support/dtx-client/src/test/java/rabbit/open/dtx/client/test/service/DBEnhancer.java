package rabbit.open.dtx.client.test.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import rabbit.open.dts.common.spring.enhance.AbstractAnnotationEnhancer;
import rabbit.open.dts.common.spring.enhance.PointCutHandler;

/**
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
@Component
@SuppressWarnings("serial")
public class DBEnhancer extends AbstractAnnotationEnhancer<Transactional> {

    @Override
    protected PointCutHandler<Transactional> getHandler() {
        return (invocation, annotation) -> {
            if (invocation.getThis() instanceof EnterpriseService) {
                EnterpriseService es = (EnterpriseService) invocation.getThis();
                es.increase();
            }
            try {
                return invocation.proceed();
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        };
    }
}
