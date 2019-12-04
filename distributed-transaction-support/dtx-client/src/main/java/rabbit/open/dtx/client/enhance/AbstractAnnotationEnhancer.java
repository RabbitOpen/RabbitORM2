package rabbit.open.dtx.client.enhance;

import org.springframework.aop.Advisor;
import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.AdvisedSupport;
import org.springframework.aop.framework.autoproxy.AbstractAutoProxyCreator;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanCreationException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.HashSet;
import java.util.Set;

/**
 * 注解增强器
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
public abstract class AbstractAnnotationEnhancer<T extends Annotation> extends AbstractAutoProxyCreator {

    private Set<String> beans2Enhance = new HashSet<>();

    private transient Object[] pointCuts;

    private Class<T> clz;

    public AbstractAnnotationEnhancer() {
        try {
            Class<?> cls = getClass();
            while (!(cls.getGenericSuperclass() instanceof ParameterizedType)) {
                cls = cls.getSuperclass();
            }
            this.clz = (Class<T>) ((ParameterizedType) (cls.getGenericSuperclass())).getActualTypeArguments()[0];
        } catch (Exception e) {
            // TO DO : ignore
        }
    }

    // 获取目标注解
    protected final Class<T> getTargetAnnotation() {
        return clz;
    }

    // 获取切面处理器
    protected abstract PointCutHandler<T> getHandler();

    /**
     * 设置切点位置
     * @author xiaoqianbin
     * @date 2019/12/3
     **/
    protected PointCutPosition getPointCutPosition() {
        return PointCutPosition.FIRST;
    }

    @Override
    protected Object[] getAdvicesAndAdvisorsForBean(Class<?> beanClass, String beanName, TargetSource customTargetSource) {
        if (beans2Enhance.contains(beanName)) {
            return getPointCuts();
        }
        return DO_NOT_PROXY;
    }

    // 如果这里不创建bean，spring就会自己创建一个bean
    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
        for (Method method : beanClass.getDeclaredMethods()) {
            if (null != method.getAnnotation(getTargetAnnotation())) {
                beans2Enhance.add(beanName);
                break;
            }
        }
        return super.postProcessBeforeInstantiation(beanClass, beanName);
    }

    private synchronized Object[] getPointCuts() {
        if (null != pointCuts) {
            return pointCuts;
        }
        pointCuts = new Object[]{new AnnotationPointCut<>(getTargetAnnotation(), getHandler())};
        return pointCuts;
    }

    // AbstractAutowireCapableBeanFactory ---> doCreateBean方法
    @Override
    protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {
        if (!beans2Enhance.contains(beanName)) {
            return super.wrapIfNecessary(bean, beanName, cacheKey);
        }
        if (!AopUtils.isAopProxy(bean)) {
            // 没有被aop包装的，就直接使用默认包装器
            return super.wrapIfNecessary(bean, beanName, cacheKey);
        } else {
            AdvisedSupport support = getAdvisedSupport(bean);
            Advisor[] advisors = buildAdvisors(beanName, getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null));
            for (int i = 0; i < advisors.length; ++i) {
                Advisor advisor = advisors[i];
                if (PointCutPosition.FIRST == getPointCutPosition()) {
                    support.addAdvisor(0, advisor);
                } else {
                    support.addAdvisor(advisor);
                }
            }
            return bean;
        }
    }

    private AdvisedSupport getAdvisedSupport(Object bean) {
        try {
            Field f;
            if (AopUtils.isJdkDynamicProxy(bean)) {
                // JdkDynamicAopProxy
                f = bean.getClass().getSuperclass().getDeclaredField("h");
            } else {
                // CglibAopProxy --> getCallBack  org.springframework.cglib.proxy.Enhancer.getCallbackField(int index)
                f = bean.getClass().getDeclaredField("CGLIB$CALLBACK_0");
            }
            f.setAccessible(true);
            Object proxy = f.get(bean);
            // 获取advised对象信息
            Field advised = proxy.getClass().getDeclaredField("advised");
            advised.setAccessible(true);
            return (AdvisedSupport) advised.get(proxy);
        } catch (Exception e) {
            throw new BeanCreationException(e.getMessage(), e);
        }
    }

    /**
     * 切点位置
     * @author xiaoqianbin
     * @date 2019/12/3
     **/
    public enum PointCutPosition {
        FIRST, LAST
    }

}
