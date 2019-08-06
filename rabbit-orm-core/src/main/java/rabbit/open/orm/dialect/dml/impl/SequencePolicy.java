package rabbit.open.orm.dialect.dml.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import rabbit.open.common.exception.RabbitDMLException;
import rabbit.open.orm.dml.DMLAdapter;
import rabbit.open.orm.dml.NonQueryAdapter;
import rabbit.open.orm.dml.PolicyInsert;
import rabbit.open.orm.dml.RabbitValueConverter;
import rabbit.open.orm.dml.meta.FieldMetaData;
import rabbit.open.orm.dml.meta.MetaData;

/**
 * <b>Description:   序列插入策略实现</b>.
 * <b>@author</b>    肖乾斌
 * 
 */
public class SequencePolicy extends PolicyInsert {

    @Override
    public <T> T insert(Connection conn, NonQueryAdapter<T> adapter, T data) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            FieldMetaData fmd = MetaData.getPrimaryKeyFieldMeta(getEntityClass(adapter));
            Field pk = fmd.getField();
            String columnName = adapter.getSessionFactory().getColumnName(fmd.getColumn());
            String sql = getSql(adapter).toString() + " returning " + columnName
                    + " into ?";
            stmt = conn.prepareStatement(sql);
            Method method = getMethod(stmt, "registerReturnParameter", new Class<?>[]{int.class, int.class});
            int index = 0;
            for (int i = 0; i < sql.length(); i++) {
                if ("?".equals(sql.substring(i, i + 1))) {
                    index++;
                }
            }
            method.invoke(stmt, index, Types.BIGINT);
            setPreparedStatementValue(adapter, stmt);
            stmt.executeUpdate();
            method = getMethod(stmt, "getReturnResultSet", null);
            rs = (ResultSet) method.invoke(stmt);
            if (rs.next()) {
                pk.setAccessible(true);
                pk.set(data, RabbitValueConverter.cast(rs.getBigDecimal(1),
                                pk.getType()));
            }
            return data;
        } catch (IllegalArgumentException | ReflectiveOperationException e) {
            throw new RabbitDMLException(e);
        } finally {
            closeResultSet(rs);
            DMLAdapter.closeStmt(stmt);
        }
    }

    private Method getMethod(PreparedStatement stmt, String methodName, Class<?>[] paramTypes)
            throws NoSuchMethodException {
        return stmt.getClass().getMethod(methodName, paramTypes);
    }
}
