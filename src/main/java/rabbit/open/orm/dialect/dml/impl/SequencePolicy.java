package rabbit.open.orm.dialect.dml.impl;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import rabbit.open.orm.annotation.Column;
import rabbit.open.orm.dml.DMLAdapter;
import rabbit.open.orm.dml.NonQueryAdapter;
import rabbit.open.orm.dml.PolicyInsert;
import rabbit.open.orm.dml.RabbitValueConverter;
import rabbit.open.orm.dml.meta.MetaData;
import rabbit.open.orm.exception.RabbitDMLException;

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
        try{
            Field pk = MetaData.getPrimaryKeyField(getEntityClass(adapter));
            String sql = getSql(adapter).toString() + " returning " + pk.getAnnotation(Column.class).value() 
                    + " into ?";
            stmt = conn.prepareStatement(sql);
            Method method = getMethod(stmt, "registerReturnParameter", new Class<?>[]{int.class, int.class});
            int index = 0;
            for(int i = 0; i < sql.length(); i++){
                if(sql.substring(i, i + 1).equals("?")){
                    index++;
                }
            }
            method.invoke(stmt, index, Types.BIGINT);
            setPreparedStatementValue(adapter, stmt);
            stmt.executeUpdate();
            method = getMethod(stmt, "getReturnResultSet", null);
            rs = (ResultSet) method.invoke(stmt);  
            if(rs.next()){
                pk.setAccessible(true);
                pk.set(data, RabbitValueConverter.cast(rs.getBigDecimal(1), pk.getType()));
            }
            return data;
        } catch (IllegalArgumentException e) {
            logger.error(e.getMessage(), e);
            throw new RabbitDMLException(e);
        } catch (ReflectiveOperationException e) {
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
