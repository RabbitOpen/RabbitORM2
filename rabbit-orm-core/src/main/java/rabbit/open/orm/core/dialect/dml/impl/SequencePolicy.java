package rabbit.open.orm.core.dialect.dml.impl;

import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.NonQueryAdapter;
import rabbit.open.orm.core.dml.PolicyInsert;
import rabbit.open.orm.core.dml.convert.RabbitValueConverter;
import rabbit.open.orm.core.dml.meta.FieldMetaData;
import rabbit.open.orm.core.dml.meta.MetaData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.*;
import java.util.List;

/**
 * <b>Description:   序列插入策略实现</b>.
 * <b>@author</b>    肖乾斌
 * 
 */
public class SequencePolicy extends PolicyInsert {

    @Override
    public <T> void insert(Connection conn, NonQueryAdapter<T> adapter, List<T> list) throws SQLException {
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
            for (T data : list) {
                if (rs.next()) {
                    pk.setAccessible(true);
                    pk.set(data, RabbitValueConverter.cast(rs.getBigDecimal(1), pk.getType()));
                }
            }
        } catch (IllegalArgumentException | ReflectiveOperationException e) {
            throw new RabbitDMLException(e);
        } finally {
            closeResultSet(rs);
            DMLObject.closeStmt(stmt);
        }
    }

    private Method getMethod(PreparedStatement stmt, String methodName, Class<?>[] paramTypes)
            throws NoSuchMethodException {
        return stmt.getClass().getMethod(methodName, paramTypes);
    }
}
