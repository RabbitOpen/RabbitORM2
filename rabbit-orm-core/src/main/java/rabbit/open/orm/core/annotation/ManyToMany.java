package rabbit.open.orm.core.annotation;

import rabbit.open.orm.common.dml.Policy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * 多对多
 * @author	肖乾斌
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface ManyToMany {

	//一端对象在中间表中的外键名
	String joinColumn();
	
	//一端对象和中间表关联的字段(默认是主键)
	String masterFieldName() default "";
	
	//中间表的名字
	String joinTable();
	
	//多端对象在中间表中的外键名
	String reverseJoinColumn();
	
	//多端对象和中间表关联的字段(默认是主键)
	String slaveFieldName() default "";
	
	Policy policy() default Policy.NONE;
	
	//策略为sequence时的sequence的名字
	String sequence() default "";
	
	//中间表的主键字段名
	String id() default "";

	//中间表中用于过滤映射关系的字段名
	String filterColumn() default "";
}
