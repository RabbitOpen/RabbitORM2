package rabbit.open.orm.core.spring;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import rabbit.open.orm.common.exception.EmptyFieldMappingException;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.annotation.NameSpace;
import rabbit.open.orm.core.annotation.Param;
import rabbit.open.orm.core.annotation.SQLName;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.dml.name.NamedSQL;
import rabbit.open.orm.core.spring.runner.MethodMapping;
import rabbit.open.orm.core.spring.runner.SQLRunner;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * <b>@description 接口实现工厂 </b>
 * @param <T>
 */
public class MapperFactory<T> implements FactoryBean<T>, InvocationHandler {

	// 被代理的接口clz
	private Class<T> interfaceClz;
	
	private ApplicationContext context;
	
	// 代理对象
	private T proxyObject;
	
	// mapper注解中的class
	private Class<?> namespaceClz;
	
	private SessionFactory factory;
	
	// 缓存方法和sqlObject name的映射关系
	private Map<Method, MethodMapping> methodMapping = new HashMap<>();
	
	public MapperFactory() {
		super();
	}
	
	public MapperFactory(Class<T> interfaceClz, ApplicationContext context) {
		super();
		this.interfaceClz = interfaceClz;
		this.context = context;
	}

	/**
	 * 	单例
	 */
	@SuppressWarnings("unchecked")
	@Override
	public T getObject() throws Exception {
		MapperFactory<T> proxy = new MapperFactory<>(interfaceClz, context);
		proxyObject = (T) Proxy.newProxyInstance(getClass().getClassLoader(), 
				new Class<?>[] {interfaceClz}, proxy);
		proxy.setProxyObject(proxyObject);
		namespaceClz = interfaceClz.getAnnotation(NameSpace.class).value();
		proxy.setNamespaceClz(namespaceClz);
		// 缓存方法关系
		doMethodMappingCache(proxy);
		return proxy.proxyObject;
	}

	private void doMethodMappingCache(MapperFactory<T> proxy) {
		Method[] methods = interfaceClz.getDeclaredMethods();
		for (Method m : methods) {
			String name = m.getName();
			SQLName mapper = m.getAnnotation(SQLName.class);
			if (null != mapper) {
				name = mapper.value();
			}
			MethodMapping mapping = new MethodMapping(name);
			//缓存参数
			for (Parameter p : m.getParameters()) {
				Param fm = p.getAnnotation(Param.class);
				if (null == fm) {
					throw new EmptyFieldMappingException(interfaceClz, m);
				}
				mapping.addParaName(fm.value());
			}
			mapping.setReturnType(m.getReturnType());
			mapping.setGenericalResultType(m.getGenericReturnType());
			proxy.cacheMapping(m, mapping);
		}
	}
	
	private void cacheMapping(Method m, MethodMapping mapping) {
		this.methodMapping.put(m, mapping);
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		String methodName = method.getName();
		if ("toString".equals(methodName)) {
			return proxy.getClass().getName();
		}
		if ("hashCode".equals(methodName)) {
			return 0;
		}
		if ("equals".equals(methodName)) {
			return true;
		}
		MethodMapping mapping = methodMapping.get(method);
		if (null == mapping) {
			throw new RabbitDMLException("no query[" + methodName + "] is defined!");
		}
		NamedSQL sqlObject = getFactory().getQueryByNameAndClass(mapping.getSqlName(), namespaceClz);
		return SQLRunner.getRunner(sqlObject.getSqlType()).run(args, mapping, namespaceClz, getFactory());
	}
	
	@Override
	public Class<?> getObjectType() {
		return interfaceClz;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	public void setInterfaceClz(Class<T> interfaceClz) {
		this.interfaceClz = interfaceClz;
	}

	public void setContext(ApplicationContext context) {
		this.context = context;
	}

	public SessionFactory getFactory() {
		if (null == factory) {
			factory = context.getBean(SessionFactory.class);
		}
		return factory;
	}
	
	public void setProxyObject(T proxyObject) {
		this.proxyObject = proxyObject;
	}
	
	public void setNamespaceClz(Class<?> namespaceClz) {
		this.namespaceClz = namespaceClz;
	}
	
}
