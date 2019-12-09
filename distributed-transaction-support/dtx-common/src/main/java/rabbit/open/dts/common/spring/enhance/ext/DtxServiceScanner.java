package rabbit.open.dts.common.spring.enhance.ext;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import rabbit.open.dts.common.spring.anno.DtxService;
import rabbit.open.dts.common.spring.anno.Namespace;
import rabbit.open.dts.common.spring.anno.Reference;
import rabbit.open.dts.common.spring.enhance.AbstractAnnotationEnhancer;
import rabbit.open.dts.common.spring.enhance.PointCutHandler;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC 调用增强
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class DtxServiceScanner extends AbstractAnnotationEnhancer<Reference> implements ApplicationContextAware {

    private transient ApplicationContext context;

    // key: namespace, value: dtxService object
    private static final Map<String, Object> dtxServiceCache = new ConcurrentHashMap<>();

    /**
     * <b>@Reference 注解对应的class和它的namespace的缓存</b>
     * key: reference class, value: namespace
     **/
    private static final Map<Class<?>, String> namespaceCache = new ConcurrentHashMap<>();

    @Override
    protected PointCutHandler<Reference> getHandler() {
        return (invocation, annotation) -> {
            // DO RPC PROXY
            Object bean = context.getBean(annotation.registryBeanName());
            return null;
        };
    }

    /**
     * 增强有@Reference注解的类
     * @param	beanClass
	 * @param	beanName
     * @author  xiaoqianbin
     * @date    2019/12/7
     **/
    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
        if (null != beanClass.getAnnotation(getTargetAnnotation())) {
            if (1 != beanClass.getInterfaces().length) {
                // 声明了Reference的class不能实现多个接口
                throw new BeanCreationException(String.format("too many interface were implemented by %s", beanClass));
            }
            Namespace namespace = beanClass.getInterfaces()[0].getAnnotation(Namespace.class);
            if (null == namespace) {
                throw new BeanCreationException(String.format("@%s can't be set on %s", Reference.class.getSimpleName(), beanClass));
            }
            namespaceCache.put(beanClass, namespace.value());
            beans2Enhance.add(beanName);
        }
        return super.postProcessBeforeInstantiation(beanClass, beanName);
    }

    /**
     * 缓存bean Service
     * @param	bean
	 * @param	beanName
     * @author  xiaoqianbin
     * @date    2019/12/7
     **/
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        DtxService dtxService = bean.getClass().getAnnotation(DtxService.class);
        if (null != dtxService) {
            // 扫描dtxService实现
            for (Class<?> anInterface : beanName.getClass().getInterfaces()) {
                Namespace namespace = anInterface.getAnnotation(Namespace.class);
                if (null != namespace) {
                    assertDuplicatedNamespace(namespace.value());
                    dtxServiceCache.put(namespace.value(), bean);
                } else {
                    throw new BeanCreationException("@" + Namespace.class.getName() + " can't be empty");
                }
            }
        }
        return super.postProcessBeforeInitialization(bean, beanName);
    }

    @Override
    protected synchronized Object[] getPointCuts() {
        if (null != pointCuts) {
            return pointCuts;
        }
        pointCuts = new Object[]{new ReferencePointCut<>(getTargetAnnotation(), getHandler())};
        return pointCuts;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.context = applicationContext;
    }

    /**
     * 断言重复的命名空间
     * @param	namespace
     * @author  xiaoqianbin
     * @date    2019/12/7
     **/
    private void assertDuplicatedNamespace(String namespace) {
        if (dtxServiceCache.containsKey(namespace)) {
            throw new BeanCreationException(String.format("duplicated dtxService namespace[%s] is found!",
                    namespace));
        }
    }

    /**
     * 根据命名空间查找对应的serviceBean
     * @param	namespace
     * @author  xiaoqianbin
     * @date    2019/12/7
     **/
    public static final Object getDtxService(String namespace) {
        return dtxServiceCache.get(namespace);
    }
}
