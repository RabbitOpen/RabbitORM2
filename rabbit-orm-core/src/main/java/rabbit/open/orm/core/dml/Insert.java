package rabbit.open.orm.core.dml;

import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.common.exception.NoField2InsertException;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.common.exception.UnSupportedOperationException;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.dml.convert.RabbitValueConverter;
import rabbit.open.orm.core.dml.meta.FieldMetaData;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.policy.UUIDPolicy;
import rabbit.open.orm.core.dml.shard.DefaultShardingPolicy;
import rabbit.open.orm.core.dml.shard.ShardFactor;
import rabbit.open.orm.core.dml.shard.ShardingPolicy;

import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

/**
 * <b>Description: 	新增操作</b><br>
 * <b>@author</b>	肖乾斌
 * @param <T>
 */
public class Insert<T> extends NonQueryAdapter<T> {

    //需要插入的对象
    protected T data;

    public Insert(SessionFactory sessionFactory, Class<T> clz, T data) {
        super(sessionFactory, clz);
        this.data = data;
        if (null == data) {
            throw new RabbitDMLException("empty data[" + data + "] can't be inserted!");
        }
        setDmlType(DMLType.INSERT);
        createInsertSql(data);
        sqlOperation = conn -> {
            updateTargetTableName();
            return doExecute(conn);
        };
    }

    /**
     * 批量add
     * @param    sessionFactory
     * @param    clz
     * @param    list
     * @author xiaoqianbin
     * @date 2020/6/1
     **/
    public Insert(SessionFactory sessionFactory, Class<T> clz, List<T> list) {
        super(sessionFactory, clz);
        if (null == list || list.isEmpty()) {
            throw new RabbitDMLException("data can't be empty!");
        }
        setDmlType(DMLType.INSERT);
        Class<? extends ShardingPolicy> shardingPolicy = getMetaData().getShardingPolicy().getClass();
        if (!DefaultShardingPolicy.class.equals(shardingPolicy)) {
            throw new UnSupportedOperationException("batch add is not supported by shardingPolicy[" + shardingPolicy.getName() + "]");
        }
        // 生成sql
        createBatchAddSql(list);
        sqlOperation = conn -> {
            FieldMetaData fmd = MetaData.getPrimaryKeyFieldMeta(getEntityClz());
            Policy policy = fmd.getPrimaryKey().policy();
            PolicyInsert insertPolicy = PolicyInsert.getInsertPolicy(policy);
            insertPolicy.insert(conn, this, list);
            return list.size();
        };
    }

    /**
     * 创建batch sql
     * @param	list
     * @author  xiaoqianbin
     * @date    2020/6/1
     **/
    private void createBatchAddSql(List<T> list) {
        StringBuilder fields = createFieldsSql();
        sql = new StringBuilder();
        sql.append("INSERT INTO " + getMetaData().getTableName() + "");
        sql.append(fields);
        sql.append(" VALUES ");
        for (T data : list) {
            sql.append(createRealValueSql(data) + ", ");
        }
        sql.deleteCharAt(sql.lastIndexOf(","));
    }

    private StringBuilder createRealValueSql(T data) {
        StringBuilder sql = new StringBuilder("(");
        for (FieldMetaData fmd : metaData.getFieldMetas()) {
            sql.append(getValueString(data, fmd));
        }
        sql.deleteCharAt(sql.lastIndexOf(","));
        sql.append(")");
        return sql;
    }

    private String getValueString(T data, FieldMetaData fmd) {
        Object value = getValue(fmd.getField(), data);
        if (null == value) {
            return "null, ";
        }
        if (fmd.isForeignKey()) {
            if ("".equals(fmd.getColumn().joinFieldName().trim())) {
                FieldMetaData foreignKey = new FieldMetaData(fmd.getForeignField(), fmd.getForeignField().getAnnotation(Column.class));
                value = getValue(foreignKey.getField(), value);
            } else {
                // 自定义外键关联
                FieldMetaData cfm = MetaData.getCachedFieldsMeta(value.getClass(), fmd.getColumn().joinFieldName().trim());
                value = getValue(cfm.getField(), value);
            }
        }
        if (fmd.isString() || fmd.getField().getType().isEnum()) {
            return String.format("'%s', ", value);
        }
        if (fmd.isDate()) {
            return String.format("'%s', ", new SimpleDateFormat(fmd.getColumn().pattern()).format(value));
        }
        return value.toString() + ", ";
    }


    /**
     * 创建需要插入的字段sql片段
     * @author  xiaoqianbin
     * @date    2020/6/1
     **/
    private StringBuilder createFieldsSql() {
        StringBuilder fields = new StringBuilder("(");
        for (FieldMetaData fmd : metaData.getFieldMetas()) {
            fields.append(getColumnName(fmd.getColumn()) + ",");
        }
        fields.deleteCharAt(fields.length() - 1);
        fields.append(")");
        return fields;
    }

    @Override
    public List<ShardFactor> getFactors() {
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
     * <b>Description:	执行sql语句</b><br>
     * @param conn
     * @return
     */
    private long doExecute(Connection conn) throws SQLException {
        FieldMetaData fmd = MetaData.getPrimaryKeyFieldMeta(getEntityClz());
        Policy policy = fmd.getPrimaryKey().policy();
        PolicyInsert insertPolicy = PolicyInsert.getInsertPolicy(policy);
        insertPolicy.insert(conn, this, Arrays.asList(data));
        showSql();
        return 1L;
    }

    /**
     * <b>Description:	创建一个Insert语句</b><br>
     * @param data
     * @return
     */
    private Insert<T> createInsertSql(T data) {
        StringBuilder fields = createFieldsSql(data);
        sql = new StringBuilder();
        sql.append("INSERT INTO " + TARGET_TABLE_NAME + "");
        sql.append(fields);
        sql.append(" VALUES ");
        sql.append(createValuesSql(data));
        return this;
    }

    /**
     * <b>Description:	创建sql的值片段</b><br>
     * @param obj
     * @return
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
            cacheForeignKeyValue(fmd, fieldValue);
            vsql.append(PLACE_HOLDER);
        } else {
            if (fmd.isPrimaryKey() && fmd.getPrimaryKey().policy().equals(Policy.SEQUENCE)) {
                vsql.append(fieldValue);
            } else {
                preparedValues.add(new PreparedValue(RabbitValueConverter.convertByField(fieldValue, fmd), fmd.getField()));
                vsql.append(PLACE_HOLDER);
            }
        }
        vsql.append(",");
        return vsql;
    }

    // 向preparedValues添加外键字段的值
    private void cacheForeignKeyValue(FieldMetaData fmd, Object value) {
        if ("".equals(fmd.getColumn().joinFieldName().trim())) {
            FieldMetaData foreignKey = new FieldMetaData(fmd.getForeignField(), fmd.getForeignField().getAnnotation(Column.class));
            Object vv = getValue(foreignKey.getField(), value);
            preparedValues.add(new PreparedValue(RabbitValueConverter.convertByField(vv, foreignKey), foreignKey.getField()));
        } else {
            // 自定义外键关联
            FieldMetaData cfm = MetaData.getCachedFieldsMeta(value.getClass(), fmd.getColumn().joinFieldName().trim());
            preparedValues.add(new PreparedValue(RabbitValueConverter.convertByField(getValue(cfm.getField(), value), cfm), cfm.getField()));
        }
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

    /**
     * 判断字段是不是 非主键，或者自增长类型的主键
     * @param    fmd
     * @author xiaoqianbin
     * @date 2020/6/1
     **/
    private boolean isIgnoreField(FieldMetaData fmd) {
        return !fmd.isPrimaryKey() || fmd.getPrimaryKey().policy().equals(Policy.NONE)
                || fmd.getPrimaryKey().policy().equals(Policy.AUTOINCREMENT);
    }

}
