package rabbit.open.orm.core.dialect.dml.impl;

import com.mysql.jdbc.Statement;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.NonQueryAdapter;
import rabbit.open.orm.core.dml.PolicyInsert;
import rabbit.open.orm.core.dml.convert.RabbitValueConverter;
import rabbit.open.orm.core.dml.meta.MetaData;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * <b>Description:   自增长插入策略实现</b>.
 * <b>@author</b>    肖乾斌
 * 
 */
public class AutoIncrementPolicy extends PolicyInsert {

    @Override
    public <T> T insert(Connection conn, NonQueryAdapter<T> adapter, T data) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        try {
            Field pk = MetaData.getPrimaryKeyField(getEntityClass(adapter));
            stmt = conn.prepareStatement(getSql(adapter).toString(), Statement.RETURN_GENERATED_KEYS);
            setPreparedStatementValue(adapter, stmt);
            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();
            String proxyStmtClz = "rabbit.open.dtx.client.datasource.proxy.TxPreparedStatement";
            if (rs.isClosed() && proxyStmtClz.equals(stmt.getClass().getName())) {
                Method getAutoIncrementId = Class.forName(proxyStmtClz).getDeclaredMethod("getAutoIncrementId");
                long id = (long) getAutoIncrementId.invoke(stmt);
                setValue2Field(data, pk, RabbitValueConverter.cast(id, pk.getType()), adapter);
            } else if (rs.next()) {
                pk.setAccessible(true);
                setValue2Field(data, pk, RabbitValueConverter.cast(rs.getBigDecimal(1), pk.getType()), adapter);
            }
            return data;
        } catch (Exception e) {
            throw new RabbitDMLException(e);
        } finally {
            closeResultSet(rs);
            DMLObject.closeStmt(stmt);
        }
    }

}
