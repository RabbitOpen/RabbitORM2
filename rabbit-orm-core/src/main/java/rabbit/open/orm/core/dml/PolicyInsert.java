package rabbit.open.orm.core.dml;

import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.dialect.dml.impl.AutoIncrementPolicy;
import rabbit.open.orm.core.dialect.dml.impl.SequencePolicy;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Description:   insert策略</b>.
 * <b>@author</b>    肖乾斌
 * 
 */
public class PolicyInsert {

    private static Map<Policy, PolicyInsert> policies = new ConcurrentHashMap<>();
    
    public static void init() {
        policies.put(Policy.NONE, new PolicyInsert());
        policies.put(Policy.UUID, new PolicyInsert());
        policies.put(Policy.AUTOINCREMENT, new AutoIncrementPolicy());
        policies.put(Policy.SEQUENCE, new SequencePolicy());
    }
    
    /**
     * 
     * <b>Description:    插入数据</b><br>.
     * @param conn
     * @param adapter
     * @param list
     * @return	
     * 
     */
    public <T> void insert(Connection conn, NonQueryAdapter<T> adapter, List<T> list) throws SQLException {
        PreparedStatement stmt = null;
        try {
            stmt = conn.prepareStatement(adapter.sql.toString());
            adapter.setPreparedStatementValue(stmt, DMLType.INSERT);
            stmt.executeUpdate();
        } finally {
            DMLObject.closeStmt(stmt);
        }
    }
    
    /**
     * 
     * <b>Description:    根据NonQueryAdapter获取实体对应的class信息</b><br>.
     * @param adapter
     * @return	
     * 
     */
    public <T> Class<T> getEntityClass(NonQueryAdapter<T> adapter) {
        return adapter.getEntityClz();
    }
    
    /**
     * 
     * <b>Description:    获取NonQueryAdapter内置的sql信息</b><br>.
     * @param adapter
     * @return	
     * 
     */
    public <T> StringBuilder getSql(NonQueryAdapter<T> adapter) {
        return adapter.sql;
    }

    public <T> void setPreparedStatementValue(NonQueryAdapter<T> adapter, PreparedStatement stmt) throws SQLException {
        adapter.setPreparedStatementValue(stmt, DMLType.INSERT);
    }

    public static PolicyInsert getInsertPolicy(Policy p) {
        if (!policies.containsKey(p)) {
            throw new RabbitDMLException("policy[" + p + "] is not registed!");
        }
        return policies.get(p);
    }
    
    /**
     * 
     * <b>Description:    关闭结果集资源</b><br>.
     * @param rs	
     * 
     */
    protected void closeResultSet(ResultSet rs) {
        if (null != rs) {
            try {
                rs.close();
            } catch (SQLException e) {
                throw new RabbitDMLException(e);
            }
        }
    }
    
    protected void setValue2Field(Object target, Field field, Object value, DMLObject<?> adapter) {
        adapter.setValue2Field(target, field, value);
    }
}
