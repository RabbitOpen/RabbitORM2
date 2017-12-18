package rabbit.open.orm.dml.meta;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import rabbit.open.orm.annotation.Column;
import rabbit.open.orm.annotation.Entity;

/**
 * 
 * 一对多或者多对多的源信息
 * @author	肖乾斌
 * 
 */
public class JoinFieldMetaData<T> {

	//在实体中对应的字段信息
	private Field field;
	
	//关联的实体类信息
	private Class<T> joinClass;
	
	//字段的注解信息
	private Annotation annotation;
	
	//联合查询时的过滤条件
	private T filter;
	
	//表名
	private String tableName;
	
	//主键名
	private String primaryKey;
	
	public String getPrimaryKey() {
		return primaryKey;
	}

	public T getFilter() {
		return filter;
	}

	public void setFilter(T filter) {
		this.filter = filter;
	}

	public Field getField() {
		return field;
	}

	public Class<?> getJoinClass() {
		return joinClass;
	}

	public Annotation getAnnotation() {
		return annotation;
	}

	public String getTableName() {
		return tableName;
	}

	public JoinFieldMetaData(Field field, Class<T> joinClass, Annotation annotation){
		super();
		this.field = field;
		this.joinClass = joinClass;
		this.annotation = annotation;
		this.tableName = findTargetTable(joinClass);
		this.primaryKey = MetaData.getPrimaryKeyField(joinClass).getAnnotation(Column.class).value();
	}
	
	private String findTargetTable(Class<?> jc){
		Class<?> clz = jc;
		while(null == clz.getAnnotation(Entity.class)){
			clz = clz.getSuperclass();
		}
		return clz.getAnnotation(Entity.class).value();
	}
}
