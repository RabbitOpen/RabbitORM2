package rabbit.open.orm.core.dml.meta;

import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.PrimaryKey;
import rabbit.open.orm.core.dml.meta.proxy.GenericAnnotationProxy;

import java.lang.reflect.Field;
import java.util.Date;

/**
 * <b>Description: 	字段元信息对象</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
public class FieldMetaData {
	
	/**
	 * <b>@description 复制一个元信息 </b>
	 * @return
	 */
	public FieldMetaData copy() {
		FieldMetaData fmd = new FieldMetaData();
		fmd.field = field;
		fmd.column = column;
		fmd.primaryKey = primaryKey;
		fmd.foreignField = foreignField;
		fmd.isPrimaryKey = isPrimaryKey;
		fmd.isForeignKey = isForeignKey;
		return fmd;
	}

	//字段在其所在的类中的Field对象
	private Field field;
	
	//字段属性的Column信息
	private Column column;
	
	//字段所在的表名
	private String fieldTableName;
		
	//字段的值
	private Object fieldValue;
	
	//是否是外键字段
	private boolean isForeignKey = false;
	
	//外键在其所在实体中的字段对象
	private Field foreignField;
	
	//是否是主键字段
	private boolean isPrimaryKey = false;

	private PrimaryKey primaryKey;
	
	//同类型字段下标
	private int index = 0;
	
	//标识是否是拥有同类型字段
	private boolean multiFetchField = false;
	
	//简单数据类型
    private String baseDataType = int.class.getName() + "|"
                       + float.class.getName() + "|"
                       + double.class.getName() + "|"
                       + short.class.getName() + "|"
                       + long.class.getName() + "|"
                       + char.class.getName() + "|"
                       + byte.class.getName() + "|"
                       + boolean.class.getName();
	
    public FieldMetaData() {}
    
    public FieldMetaData(Field field, Column column) {
		super();
		this.field = field;
		this.column = GenericAnnotationProxy.proxy(column, Column.class);
		if (baseDataType.contains(field.getType().getSimpleName())) {
			throw new RabbitDMLException("data type[" + field.getType().getSimpleName()
					+ "] is not supported by rabbit entity!");
		}
		setPrimaryKey(GenericAnnotationProxy.proxy(field.getAnnotation(PrimaryKey.class), PrimaryKey.class));
		//判断是否是外键类型
		if (!MetaData.isEntityClass(field.getType())) {
			return;
		}
		this.isForeignKey = true;
		if ("".equals(this.column.joinFieldName().trim())) {
			foreignField = MetaData.getPrimaryKeyField(field.getType());
		} else {
			foreignField = MetaData.getCachedFieldsMeta(field.getType(), this.column.joinFieldName().trim()).getField();
		}
	}
	
	public FieldMetaData(Field field, Column column, Object value, String tableName) {
		this(field, column);
		this.fieldValue = value;
		this.fieldTableName = tableName;
	}
	
	public Field getField() {
        return field;
    }

    public Column getColumn() {
        return column;
    }
	
	public boolean isMultiFetchField() {
        return multiFetchField;
    }

    public void setMultiFetchField(boolean multiFetchField) {
        this.multiFetchField = multiFetchField;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

	public boolean isForeignKey() {
		return isForeignKey;
	}

	public boolean isPrimaryKey() {
		return isPrimaryKey;
	}

	private void setPrimaryKey(PrimaryKey primaryKey) {
		this.isPrimaryKey = isPrimaryKey(primaryKey);
		this.primaryKey = primaryKey;
	}

    private boolean isPrimaryKey(PrimaryKey primaryKey) {
        return null != primaryKey;
    }

	public PrimaryKey getPrimaryKey() {
		return primaryKey;
	}

	public String getFieldTableName() {
		return fieldTableName;
	}

	public Object getFieldValue() {
		return fieldValue;
	}

	public Field getForeignField() {
		return foreignField;
	}
	
	//字符类型字段varchar
	public boolean isString() {
		return field.getType().equals(String.class);
	}

	//日期类型字段
	public boolean isDate() {
		return field.getType().equals(Date.class);
	}
	
	public void setFieldValue(Object fieldValue) {
		this.fieldValue = fieldValue;
	}
	
	public void setFieldTableName(String fieldTableName) {
		this.fieldTableName = fieldTableName;
	}

}
