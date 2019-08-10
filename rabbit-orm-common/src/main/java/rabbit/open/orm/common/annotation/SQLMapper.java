package rabbit.open.orm.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <b>@description 声明一个sql </b>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
public @interface SQLMapper {

	/**
	 * <b>@description 对应的sql名字</b>
	 * @return
	 */
	public String value();
}
