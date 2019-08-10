package rabbit.open.orm.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <b>@description 声明一个mapper </b>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface Mapper {

	/**
	 * <b>@description 该mapper对应的实体class </b>
	 * @return
	 */
	public Class<?> value();
}
