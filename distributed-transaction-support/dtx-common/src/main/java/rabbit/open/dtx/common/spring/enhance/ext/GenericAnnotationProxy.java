package rabbit.open.dtx.common.spring.enhance.ext;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 代理注解
 * @author xiaoqianbin
 * @date 2019/12/9
 **/
public class GenericAnnotationProxy implements MethodInterceptor {

    // 函数值缓存
    private Map<String, Object> map = new HashMap<>();

    private void cacheValues(Annotation annotation) {
        for (Method m : annotation.getClass().getDeclaredMethods()) {
            try {
                map.put(m.getName(), m.invoke(annotation));
            } catch (Exception e) {
                // TO DO: ignore all exception
            }
        }
    }

    /**
     *
     * <b>Description: 代理所有ManyToMany注解</b><br>
     * @param annotation
     * @return
     *
     */
    @SuppressWarnings("unchecked")
    public static <D extends Annotation> D proxy(D annotation, Class<D> clz) {
        GenericAnnotationProxy proxy = new GenericAnnotationProxy();
        proxy.cacheValues(annotation);
        Enhancer eh = new Enhancer();
        eh.setCallback(proxy);
        eh.setSuperclass(clz);
        return (D) eh.create();
    }

    /***
     * 代理session close方法，实现释放连接的功能
     */
    @Override
    public final Object intercept(Object obj, Method method, Object[] args,
                                  MethodProxy methodproxy) throws Throwable {
        if (map.containsKey(method.getName())) {
            return map.get(method.getName());
        }
        return null;
    }

}
