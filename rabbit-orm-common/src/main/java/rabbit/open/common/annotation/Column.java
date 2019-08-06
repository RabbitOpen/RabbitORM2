package rabbit.open.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * 标记字段
 * @author 肖乾斌
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.FIELD)
public @interface Column {

	public String value();

	// 日期类型数据格式
	public String pattern() default "yyyy-MM-dd HH:mm:ss";

	// 默认字符型字段长度
	public int length() default 50;

	// 标识该字段名是SQL关键字
	public boolean keyWord() default false;

	// 标识该字段为动态字段(数据库中不存在的， 典型的count(1) as num)
	public boolean dynamic() default false;
}
