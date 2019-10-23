package rabbit.open.orm.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * 一对多映射
 * @author 肖乾斌
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface OneToMany {

	// 多端对象和当前主表关联的字段
	public String joinColumn();
	
	// 一端对象和多端对象关联的字段(默认是主键)
	public String masterFieldName() default "";
}
