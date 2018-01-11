package rabbit.open.orm.exception;

@SuppressWarnings("serial")
public class AmbiguousDependencyException extends RuntimeException{

	public AmbiguousDependencyException(Class<?> clz) {
		super("ambiguous operation[addFilter] with class [" + clz.getName() + "]");
	}
	
	/**
	 * target ---> dependency的依赖路径有多条。dependency很可能是multifetch类型
	 * @param target
	 * @param dependency
	 */
	public AmbiguousDependencyException(Class<?> target, Class<?> dependency) {
	    super("ambiguous dependency [" + target.getName() + " ----> " + dependency.getName() + "] is found! ");
	}
	
}
