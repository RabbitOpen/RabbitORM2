package rabbit.open.orm.core.dml.meta;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;

import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.ManyToMany;
import rabbit.open.orm.core.annotation.OneToMany;

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
	
	//关联表中的字段的column信息
	private Column joinClassPrimaryKeyColumn;
	
	//主表中的字段的column信息
	private Column masterClassPrimaryKeyColumn;
	
	// 【主表】和【中间表】关联的字段
	private Field masterField;

	// 【从表】和【中间表】表关联的字段
	private Field slaveField;
	
	public JoinFieldMetaData(Field field, Class<T> joinClass, Class<?> targetClass, Annotation annotation) {
        super();
        this.field = field;
        this.targetClass = targetClass;
        this.joinClass = joinClass;
        this.annotation = annotation;
        this.tableName = MetaData.getTableNameByClass(joinClass);
        initPrimaryKeyColumn(joinClass, targetClass, annotation);
    }

	private void initPrimaryKeyColumn(Class<T> joinClass, Class<?> targetClass, Annotation annotation) {
		if (annotation instanceof ManyToMany) {
        	ManyToMany mtm = (ManyToMany) annotation;
        	if ("".equals(mtm.slaveFieldName().trim())) {
        		joinClassPrimaryKeyColumn = MetaData.getPrimaryKeyFieldMeta(joinClass).getColumn();
        		slaveField = MetaData.getPrimaryKeyField(joinClass);
        	} else {
        		joinClassPrimaryKeyColumn = MetaData.getCachedFieldsMeta(joinClass, mtm.slaveFieldName().trim()).getColumn();
        		slaveField = MetaData.getCachedFieldsMeta(joinClass, mtm.slaveFieldName().trim()).getField();
        	}
        	if ("".equals(mtm.masterFieldName().trim())) {
        		masterClassPrimaryKeyColumn = MetaData.getPrimaryKeyFieldMeta(targetClass).getColumn();
        		masterField = MetaData.getPrimaryKeyField(targetClass);
        	} else {
        		masterClassPrimaryKeyColumn = MetaData.getCachedFieldsMeta(targetClass, mtm.masterFieldName().trim()).getColumn();
        		masterField = MetaData.getCachedFieldsMeta(targetClass, mtm.masterFieldName().trim()).getField();
        	}
        }
		if (annotation instanceof OneToMany) {
			OneToMany otm = (OneToMany) annotation;
			if ("".equals(otm.masterFieldName().trim())) {
				masterClassPrimaryKeyColumn = MetaData.getPrimaryKeyFieldMeta(targetClass).getColumn();
				masterField = MetaData.getPrimaryKeyField(targetClass);
			} else {
				masterClassPrimaryKeyColumn = MetaData.getCachedFieldsMeta(targetClass, otm.masterFieldName().trim()).getColumn();
				masterField = MetaData.getCachedFieldsMeta(targetClass, otm.masterFieldName().trim()).getField();
			}
		}
	}
	
	public Field getMasterField() {
		return masterField;
	}
	
	public Field getSlaveField() {
		return slaveField;
	}
	
    @SuppressWarnings("unchecked")
	public JoinFieldMetaData<T> clone() {
        try {
            return (JoinFieldMetaData<T>) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RabbitDMLException(e);
        }
    }
	
	public Column getJoinClassPrimaryKeyColumn() {
		return joinClassPrimaryKeyColumn;
	}

	public Column getMasterClassPrimaryKeyColumn() {
		return masterClassPrimaryKeyColumn;
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
