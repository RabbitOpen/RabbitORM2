package rabbit.open.orm.common.exception;

import java.lang.reflect.Method;

/**
 * <b>Description mapper接口的方法参数注解为空 </b>.
 */
@SuppressWarnings("serial")
public class EmptyFieldMappingException extends RuntimeException {

	public EmptyFieldMappingException(Class<?> clz, Method method) {
		super("FieldMapper can't be empty --> " +  clz.getName() + "." + method.getName());
	}

}
