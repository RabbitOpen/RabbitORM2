package agent.rabbit.open;

import java.lang.reflect.Method;

/**
 * 委托处理器
 */
public interface DelegationHandler<T> extends EnhanceRemission {

	/**
	 * 前置处理方法
	 * @param method 当前运行的方法
	 * @param args   当前方法的参数
	 * @return
	 */
	public T before(Method method, Object[] args);

	/**
	 * 异常处理
	 * @param context before方法的返回值
	 * @param t       异常对象
	 * @param method  当前运行的方法
	 * @param args    当前方法的参数
	 */
	public void error(T context, Throwable t, Method method, Object[] args);

	/**
	 * 后置处理方法
	 * @param context 前置处理方法的输出
	 * @param method  当前运行的方法
	 * @param args    当前方法的参数
	 */
	public void after(T context, Method method, Object[] args);
}
