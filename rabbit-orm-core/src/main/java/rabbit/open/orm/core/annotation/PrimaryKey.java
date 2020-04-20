package rabbit.open.orm.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import rabbit.open.orm.common.dml.Policy;

/**
 * 
 * 标记主键字段
 * @author 肖乾斌
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface PrimaryKey {

	Policy policy() default Policy.NONE;

	// 策略为sequence时的sequence的名字
	String sequence() default "";
}
