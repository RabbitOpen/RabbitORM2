package agent.rabbit.open;

import net.bytebuddy.implementation.bind.annotation.*;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * 代理委托对象
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class AgentDelegator {
	
	private AgentDelegator() {}

	private static LinkedBlockingDeque<DelegationHandler> handlers = new LinkedBlockingDeque<>(100);

	/**
	 * 添加一个处理器
	 * 
	 * @param handler
	 * @return
	 */
	public static boolean addDelegationHandler(DelegationHandler handler) {
		return handlers.add(handler);
	}

	/**
	 * 代理方法
	 * 
	 * @param method
	 * @param callable
	 * @param args
	 * @param obj
	 */
	@RuntimeType
	public static Object intercept(@Origin Method method, @SuperCall Callable<?> callable, @AllArguments Object[] args,
			@This Object obj) throws Exception {
		if (obj instanceof EnhanceRemission) {
			return callable.call();
		}
		Map<DelegationHandler, Object> result = new HashMap<>();
		handlers.forEach(h -> result.put(h, h.before(method, args)));
		try {
			// 原有函数执行
			Object ret = callable.call();
			handlers.forEach(h -> h.after(result.get(h), method, args));
			return ret;
		} catch (Exception t) {
			handlers.forEach(h -> h.error(result.get(h), t, method, args));
			throw t;
		}
	}
}
