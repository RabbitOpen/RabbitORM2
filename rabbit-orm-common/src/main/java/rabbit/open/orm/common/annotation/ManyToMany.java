package rabbit.open.orm.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import rabbit.open.orm.common.dml.Policy;

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
	public String joinColumn();
	
	//一端对象和中间表关联的字段(默认是主键)
	public String masterFieldName() default "";
	
	//中间表的名字
	public String joinTable();
	
	//多端对象在中间表中的外键名
	public String reverseJoinColumn();
	
	//多端对象和中间表关联的字段(默认是主键)
	public String slaveFieldName() default "";
	
	public Policy policy() default Policy.NONE;
	
	//策略为sequence时的sequence的名字
	public String sequence() default "";
	
	//中间表的主键字段名
	public String id() default "";

	//中间表中用于过滤映射关系的字段名
	public String filterColumn() default "";
}
