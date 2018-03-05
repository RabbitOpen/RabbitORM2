package rabbit.open.orm.dialect.dml.impl;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import rabbit.open.orm.dml.DMLAdapter;
import rabbit.open.orm.dml.NonQueryAdapter;
import rabbit.open.orm.dml.PolicyInsert;
import rabbit.open.orm.dml.RabbitValueConverter;
import rabbit.open.orm.dml.meta.MetaData;
import rabbit.open.orm.exception.RabbitDMLException;

import com.mysql.jdbc.Statement;

/**
 * <b>Description:   自增长插入策略实现</b>.
 * <b>@author</b>    肖乾斌
 * 
 */
public class AutoIncrementPolicy extends PolicyInsert{

    @Override
    public <T> T insert(Connection conn, NonQueryAdapter<T> adapter, T data) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try{
            Field pk = MetaData.getPrimaryKeyField(getEntityClass(adapter));
            stmt = conn.prepareStatement(getSql(adapter).toString(), Statement.RETURN_GENERATED_KEYS);
            setPreparedStatementValue(adapter, stmt);
            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();
            if(rs.next()){
                pk.setAccessible(true);
                pk.set(data, RabbitValueConverter.cast(rs.getBigDecimal(1), pk.getType()));
            }
            return data;
        } catch (SQLException e){
            throw e;
        } catch (Exception e){
            throw new RabbitDMLException(e);
        } finally {
            closeResultSet(rs);
            DMLAdapter.closeStmt(stmt);
        }
    }

}
