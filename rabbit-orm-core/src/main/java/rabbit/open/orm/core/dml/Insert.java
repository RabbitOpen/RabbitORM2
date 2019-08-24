package rabbit.open.orm.core.dml;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.common.exception.NoField2InsertException;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.common.shard.ShardFactor;
import rabbit.open.orm.core.dml.filter.PreparedValue;
import rabbit.open.orm.core.dml.meta.FieldMetaData;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.policy.UUIDPolicy;

/**
 * <b>Description: 	新增操作</b><br>
 * <b>@author</b>	肖乾斌
 * @param <T>
 * 
 */
public class Insert<T> extends NonQueryAdapter<T> {

    //需要插入的对象
	protected T data;
	
	public Insert(SessionFactory sessionFactory, Class<T> clz, T data) {
        super(sessionFactory, clz);
        this.data = data;
        if (null == data) {
            throw new RabbitDMLException("empty data[" + data
                    + "] can't be inserted!");
        }
        setDmlType(DMLType.INSERT);
        createInsertSql(data);
        sqlOperation = new SQLOperation() {
            @Override
            public long executeSQL(Connection conn) throws SQLException {
                updateTargetTableName();
                return doExecute(conn);
            }
        };
	}

	@Override
    protected List<ShardFactor> getFactors() {
	    if (!factors.isEmpty()) {
	        return factors;
	    }
        for (Object o : preparedValues) {
            PreparedValue pv = (PreparedValue) o;
            factors.add(new ShardFactor(pv.getField(), FilterType.EQUAL.value(), pv.getValue()));
        }
        return factors;
    }
	
	/**
	 * 
	 * <b>Description:	执行sql语句</b><br>
	 * @param conn
	 * @return	
	 * 
	 */
	private long doExecute(Connection conn) throws SQLException {
		FieldMetaData fmd = MetaData.getPrimaryKeyFieldMeta(getEntityClz());
        Policy policy = fmd.getPrimaryKey().policy();
        PolicyInsert insertPolicy = PolicyInsert.getInsertPolicy(policy);
        data = insertPolicy.insert(conn, this, data);
        showSql();
        return 1L;
	}

	/**
	 * 
	 * <b>Description:	创建一个Insert语句</b><br>
	 * @param data
	 * @return
	 * 
	 */
	private Insert<T> createInsertSql(T data) {
		StringBuilder fields = createFieldsSql(data);
		sql = new StringBuilder();
		sql.append("INSERT INTO " +  TARGET_TABLE_NAME + "" );
		sql.append(fields);
		sql.append(" VALUES ");
		sql.append(createValuesSql(data));
		return this;
	}
	
	/**
	 * 
	 * <b>Description:	创建sql的值片段</b><br>
	 * @param obj
	 * @return
	 * 
	 */
	private StringBuilder createValuesSql(T obj) {
        StringBuilder values = new StringBuilder("(");
        for (FieldMetaData fmd : metaData.getFieldMetas()) {
            values.append(createValueSqlByMeta(obj, fmd));
        }
        values.deleteCharAt(values.length() - 1);
        values.append(")");
        return values;
	}

    private StringBuilder createValueSqlByMeta(T obj, FieldMetaData fmd) {
        StringBuilder vsql = new StringBuilder();
        Object fieldValue = getValue(fmd.getField(), obj);
        if (null == fieldValue) {
            if (!fmd.isPrimaryKey()) {
                return vsql;
            }
            if (null == (fieldValue = getPrimaryKeyValueByPolicy(fmd))) {
                return vsql;
            }
        }
        if (fmd.isForeignKey()) {
            createForeignKeySqlSegment(vsql, fmd, fieldValue);
        } else {
            if (fmd.isPrimaryKey() && fmd.getPrimaryKey().policy().equals(Policy.SEQUENCE)) {
                vsql.append(fieldValue);
            } else {
                preparedValues.add(new PreparedValue(RabbitValueConverter
                        .convert(fieldValue, fmd), fmd.getField()));
                vsql.append(PLACE_HOLDER);
            }
        }
        vsql.append(",");
        return vsql;
    }

	private void createForeignKeySqlSegment(StringBuilder values,
			FieldMetaData fmd, Object value) {
		FieldMetaData foreignKey = new FieldMetaData(fmd.getForeignField(), fmd.getForeignField().getAnnotation(Column.class));
		Object vv = getValue(foreignKey.getField(), value);
		preparedValues.add(new PreparedValue(RabbitValueConverter.convert(vv, foreignKey), foreignKey.getField()));
		values.append(PLACE_HOLDER);
	}

	private Object getPrimaryKeyValueByPolicy(FieldMetaData fmd) {
        Object value = null;
        if (fmd.getPrimaryKey().policy().equals(Policy.UUID)) {
            value = UUIDPolicy.getID();
            setValue2Field(data, fmd.getField(), value);
        } else if (fmd.getPrimaryKey().policy().equals(Policy.SEQUENCE)) {
            value = fmd.getPrimaryKey().sequence() + ".NEXTVAL";
        } else if (fmd.getPrimaryKey().policy().equals(Policy.AUTOINCREMENT)) {
            return null;
        }
        return value;
	}
	
	//创建需要插入的字段sql片段
    private StringBuilder createFieldsSql(T obj) {
        StringBuilder fields = new StringBuilder("(");
        long nonEmptyFields = 0;
        for (FieldMetaData fmd : metaData.getFieldMetas()) {
            Object value = getValue(fmd.getField(), obj);
            if (null == value && isIgnoreField(fmd)) {
                continue;
            }
            nonEmptyFields++;
            fields.append(getColumnName(fmd.getColumn()) + ",");
        }
        if (0 == nonEmptyFields) {
            throw new NoField2InsertException();
        }
        fields.deleteCharAt(fields.length() - 1);
        fields.append(")");
        return fields;
    }

	//非主键，或者自增长类型的主键
    private boolean isIgnoreField(FieldMetaData fmd) {
        if (!fmd.isPrimaryKey()) {
            return true;
        }
        if (fmd.getPrimaryKey().policy().equals(Policy.NONE)) {
            return true;
        }
        if (fmd.getPrimaryKey().policy().equals(Policy.AUTOINCREMENT)) {
            return true;
        }
        return false;
    }

}
