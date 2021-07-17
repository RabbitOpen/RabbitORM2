package rabbit.open.orm.core.annotation;

import rabbit.open.orm.core.dml.policy.PagePolicy;
import rabbit.open.orm.core.dml.shard.DefaultShardingPolicy;
import rabbit.open.orm.core.dml.shard.ShardingPolicy;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记表实体
 * 
 * @author 肖乾斌
 * 
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface Entity {

	// 表名
	String value();

	// 是否参与ddl
	boolean ddlIgnore() default false;

	// 分表策略实现
	Class<? extends ShardingPolicy> shardingPolicy() default DefaultShardingPolicy.class;

	// 设置关心的字段，用于查询过滤
	String[] concern() default {};

	// 分片表分页策略
	PagePolicy pagePolicy() default PagePolicy.DEFAULT;
	
	// 分页排序字段名称
	String orderIndexFieldName() default "";

	// 开启自动推测后 基础数据类型的字段的column注解就可以省略
	boolean autoSpeculate() default false;
}
