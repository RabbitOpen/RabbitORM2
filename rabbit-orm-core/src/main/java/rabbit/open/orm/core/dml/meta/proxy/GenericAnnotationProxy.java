package rabbit.open.orm.core.dml.meta.proxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

/**
 * <b>Description 自定义的通用注解代理</b>
 */
public class GenericAnnotationProxy implements MethodInterceptor {

    // 函数值缓存
    private Map<String, Object> map = new HashMap<>();

    public void setRealObject(Annotation annotation) {
    	for (Method m : annotation.getClass().getDeclaredMethods()) {
    		try {
    			map.put(m.getName(), m.invoke(annotation));
    		} catch (Exception e) {
				// TO DO
    			// ignore all exception
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
        if (null == annotation) {
            return null;
        }
        GenericAnnotationProxy proxy = new GenericAnnotationProxy();
        proxy.setRealObject(annotation);
        Enhancer eh = new Enhancer();
        eh.setSuperclass(clz);
        eh.setCallback(proxy);
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
