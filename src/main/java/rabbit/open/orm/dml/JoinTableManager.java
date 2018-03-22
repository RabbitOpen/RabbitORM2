package rabbit.open.orm.dml;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import rabbit.open.orm.annotation.ManyToMany;
import rabbit.open.orm.dml.filter.DMLType;
import rabbit.open.orm.dml.filter.PreparedValue;
import rabbit.open.orm.dml.meta.FieldMetaData;
import rabbit.open.orm.dml.meta.JoinFieldMetaData;
import rabbit.open.orm.dml.meta.MetaData;
import rabbit.open.orm.dml.meta.PreparedSqlDescriptor;
import rabbit.open.orm.exception.EmptyPrimaryKeyValueException;
import rabbit.open.orm.exception.RabbitDMLException;
import rabbit.open.orm.pool.SessionFactory;
import rabbit.open.orm.shard.ShardFactor;

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
                executeBatch(conn, psds, 0);
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
                executeBatch(conn, psds, 0);
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
                    showSql();
                    stmt = conn.prepareStatement(sql.toString());
                    setPreparedStatementValue(stmt, null);
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
                List<PreparedSqlDescriptor> psds2r = createRemoveJoinRecordsSql(
                        data, value);
                int counter = executeBatch(conn, psds2r, 0);
                List<PreparedSqlDescriptor> psds = createAddJoinRecordsSql(
                        data, value);
                executeBatch(conn, psds, counter);
                return 0;
            }
        };
        setDmlType(DMLType.UPDATE);
        execute();
    }
    
}
