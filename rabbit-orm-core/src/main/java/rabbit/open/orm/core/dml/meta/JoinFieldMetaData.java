package rabbit.open.orm.core.dml.meta;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.exception.RabbitDMLException;

/**
 * 
 * 一对多或者多对多的源信息
 * @author	肖乾斌
 * 
 */
public class JoinFieldMetaData<T> implements Cloneable {
    
	//在实体中对应的字段信息
	private Field field;
	
	//多端关联的实体类信息
	private Class<T> joinClass;

	//一端关联的实体
	private Class<?> targetClass;
	
	//字段的注解信息
	private Annotation annotation;
	
	//联合查询时的过滤条件
	private T filter;
	
	//字段依赖
	private Field[] dependencyFields;
	
	//表名
	private String tableName;
	
	//主键名
	private Column joinClassPrimaryKey;
	
	public JoinFieldMetaData(Field field, Class<T> joinClass, Class<?> targetClass, Annotation annotation) {
        super();
        this.field = field;
        this.targetClass = targetClass;
        this.joinClass = joinClass;
        this.annotation = annotation;
        this.tableName = MetaData.getTableNameByClass(joinClass);
        this.joinClassPrimaryKey = MetaData.getPrimaryKeyFieldMeta(joinClass).getColumn();
    }
	
	@Override
    @SuppressWarnings("unchecked")
	public JoinFieldMetaData<T> clone() {
        try {
            return (JoinFieldMetaData<T>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RabbitDMLException(e);
        }
    }
	
	public Column getJoinClassPrimaryKey() {
		return joinClassPrimaryKey;
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

	public Class<?> getTargetClass() {
        return targetClass;
    }

    public Field[] getDependencyFields() {
        return dependencyFields;
    }

    public void setDependencyFields(Field[] dependencyFields) {
        this.dependencyFields = dependencyFields;
    }
	
}
