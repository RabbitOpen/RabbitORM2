package rabbit.open.orm.core.dialect.dml.impl;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.mysql.jdbc.Statement;

import rabbit.open.orm.core.dml.DMLAdapter;
import rabbit.open.orm.core.dml.NonQueryAdapter;
import rabbit.open.orm.core.dml.PolicyInsert;
import rabbit.open.orm.core.dml.RabbitValueConverter;
import rabbit.open.orm.core.dml.meta.MetaData;

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
            Field pk = MetaData.getPrimaryKeyFieldMeta(getEntityClass(adapter)).getField();
            stmt = conn.prepareStatement(getSql(adapter).toString(),
                    Statement.RETURN_GENERATED_KEYS);
            setPreparedStatementValue(adapter, stmt);
            stmt.executeUpdate();
            rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                pk.setAccessible(true);
                setValue2Field(data, pk, RabbitValueConverter.cast(rs.getBigDecimal(1),
                                pk.getType()), adapter);
            }
            return data;
        } finally {
            closeResultSet(rs);
            DMLAdapter.closeStmt(stmt);
        }
    }

}
