package rabbit.open.orm.core.dml;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import rabbit.open.orm.common.annotation.ManyToMany;
import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.common.exception.EmptyPrimaryKeyValueException;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.common.shard.ShardFactor;
import rabbit.open.orm.core.dml.filter.PreparedValue;
import rabbit.open.orm.core.dml.meta.FieldMetaData;
import rabbit.open.orm.core.dml.meta.JoinFieldMetaData;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.meta.PreparedSqlDescriptor;

/**
 * <b>Description: 中间表管理器</b><br>
 * <b>@author</b> 肖乾斌
 * @param <T>
 * 
 */
public class JoinTableManager<T> extends NonQueryAdapter<T> {

    public JoinTableManager(SessionFactory sessionFactory, Class<T> clz) {
        super(sessionFactory, clz);
        setDmlType(DMLType.UPDATE);
    }

    /**
     * 
     * <b>Description: 向many2many的中间表中插入数据</b><br>
     * @param data
     * @return
     * 
     */
    public long addJoinRecords(T data) {
        Field pk = MetaData.getPrimaryKeyField(getEntityClz());
        Object value;
        value = getValue(pk, data);
        assertEmptyPKValue(value);
        this.sqlOperation = new SQLOperation() {

            @Override
            public long executeSQL(Connection conn) throws SQLException {
                List<PreparedSqlDescriptor> psds = createAddJoinRecordsSql(
                        data, value);
                executeBatch(conn, psds);
                return 0;
            }
        };
        setDmlType(DMLType.INSERT);
        return execute();
    }

    private void assertEmptyPKValue(Object value) {
        if (null == value) {
            throw new EmptyPrimaryKeyValueException();
        }
    }

    /**
     * 用主表名替代中间表名
     */
    @Override
    protected String getCurrentTableName() {
        return getDeclaredTableName();
    }

    @Override
    protected List<ShardFactor> getFactors() {
        return new ArrayList<>();
    }

    /**
     * 
     * <b>Description: 从many2many的中间表中移除数据</b><br>
     * @param data
     * @return
     * 
     */
    public long removeJoinRecords(T data) {
        Field pk = MetaData.getPrimaryKeyField(getEntityClz());
        Object value = getValue(pk, data);
        assertEmptyPKValue(value);
        this.sqlOperation = new SQLOperation() {
            @Override
            public long executeSQL(Connection conn) throws SQLException {
                List<PreparedSqlDescriptor> psds = createRemoveJoinRecordsSql(
                        data, value);
                if (psds.isEmpty()) {
                    throw new RabbitDMLException("no record to remove!");
                }
                executeBatch(conn, psds);
                return 0;
            }
        };
        setDmlType(DMLType.DELETE);
        return execute();
    }

    /**
     * 
     * <b>Description: 清除多对多记录</b><br>
     * @param data
     * @param join
     * 
     */
    public void clearJoinRecords(T data, Class<?> join) {
        Field pk = MetaData.getPrimaryKeyField(getEntityClz());
        Object value;
        value = getValue(pk, data);
        assertEmptyPKValue(value);
        sql = new StringBuilder();
        for (JoinFieldMetaData<?> jfm : metaData.getJoinMetas()) {
            if (!(jfm.getAnnotation() instanceof ManyToMany)
                    || !jfm.getJoinClass().equals(join)) {
                continue;
            }
            ManyToMany mtm = (ManyToMany) jfm.getAnnotation();
            sql.append("DELETE FROM " + mtm.joinTable() + WHERE);
            sql.append(mtm.joinColumn() + " = ");
            sql.append(PLACE_HOLDER);
            FieldMetaData fmd = getPrimayKeyFieldMeta(getEntityClz());
            preparedValues.add(new PreparedValue(RabbitValueConverter.convert(
                    value, fmd), fmd.getField()));
        }
        if (0 == sql.length()) {
            throw new RabbitDMLException("no record to clear!");
        }
        this.sqlOperation = new SQLOperation() {
            @Override
            public long executeSQL(Connection conn) throws SQLException {
                PreparedStatement stmt = null;
                try {
                    stmt = conn.prepareStatement(sql.toString());
                    setPreparedStatementValue(stmt, null);
                    showSql();
                    return stmt.executeUpdate();
                } finally {
                    closeStmt(stmt);
                }
            }
        };
        setDmlType(DMLType.DELETE);
        execute();
    }

    /**
     * 
     * <b>Description: 从many2many的中间表中替换数据(先移除相同的再添加)</b><br>
     * @param data
     * 
     */
    public void replaceJoinRecords(T data) {
        Field pk = MetaData.getPrimaryKeyField(getEntityClz());
        Object value = getValue(pk, data);
        assertEmptyPKValue(value);
        this.sqlOperation = new SQLOperation() {
            @Override
            public long executeSQL(Connection conn) throws SQLException {
                List<PreparedSqlDescriptor> del = createRemoveJoinRecordsSql(
                        data, value);
                executeBatch(conn, del);
                List<PreparedSqlDescriptor> add = createAddJoinRecordsSql(
                        data, value);
                executeBatch(conn, add);
                return 0;
            }
        };
        setDmlType(DMLType.UPDATE);
        execute();
    }
    
}
