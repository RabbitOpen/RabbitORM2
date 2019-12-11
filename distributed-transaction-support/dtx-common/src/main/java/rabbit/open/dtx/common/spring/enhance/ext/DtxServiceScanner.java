package rabbit.open.dtx.common.spring.enhance.ext;

import org.springframework.beans.factory.BeanCreationException;
import rabbit.open.dtx.common.spring.anno.DtxService;
import rabbit.open.dtx.common.spring.anno.Namespace;
import rabbit.open.dtx.common.spring.enhance.AbstractAnnotationEnhancer;
import rabbit.open.dtx.common.spring.enhance.PointCutHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC 调用增强
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class DtxServiceScanner extends AbstractAnnotationEnhancer<DtxService> {

    // key: namespace, value: dtxService object
    private static final Map<String, Object> dtxServiceCache = new ConcurrentHashMap<>();

    /**
     * 缓存bean Service
     * @param bean
     * @param beanName
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        DtxService dtxService = bean.getClass().getAnnotation(DtxService.class);
        if (null != dtxService) {
            // 扫描dtxService实现
            for (Class<?> anInterface : getAllInterfaces(bean.getClass())) {
                Namespace namespace = anInterface.getAnnotation(Namespace.class);
                if (null != namespace) {
                    assertDuplicatedNamespace(namespace.value());
                    dtxServiceCache.put(namespace.value(), bean);
                }
            }
        }
        return super.postProcessBeforeInitialization(bean, beanName);
    }

    private List<Class<?>> getAllInterfaces(Class<?> beanClz) {
        List<Class<?>> allInterfaces = new ArrayList<>();
        Class<?> clz = beanClz;
        while (!clz.equals(Object.class)) {
            allInterfaces.addAll(Arrays.asList(clz.getInterfaces()));
            clz = clz.getSuperclass();
        }
        return allInterfaces;
    }

    @Override
    protected PointCutHandler<DtxService> getHandler() {
        return null;
    }

    /**
     * 断言重复的命名空间
     * @param namespace
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    private void assertDuplicatedNamespace(String namespace) {
        if (dtxServiceCache.containsKey(namespace)) {
            throw new BeanCreationException(String.format("duplicated dtxService namespace[%s] is found!",
                    namespace));
        }
    }

    /**
     * 根据命名空间查找对应的serviceBean
     * @param namespace
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public static final Object getDtxService(String namespace) {
        return dtxServiceCache.get(namespace);
    }

}
