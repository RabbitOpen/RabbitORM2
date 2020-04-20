package rabbit.open.orm.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <b>@description 映射sql参数 </b>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.PARAMETER)
public @interface Param {

	/**
	 * <b>@description 对应的参数名字</b>
	 * @return
	 */
	String value();
}
