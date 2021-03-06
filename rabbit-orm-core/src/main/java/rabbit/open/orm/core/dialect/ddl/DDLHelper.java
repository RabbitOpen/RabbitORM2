package rabbit.open.orm.core.dialect.ddl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import rabbit.open.orm.common.ddl.DDLType;
import rabbit.open.orm.common.ddl.JoinTableDescriptor;
import rabbit.open.orm.common.dialect.DialectType;
import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.common.exception.RabbitDDLException;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.ManyToMany;
import rabbit.open.orm.core.dialect.ddl.impl.*;
import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.dml.meta.FieldMetaData;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.utils.SQLFormater;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>Description: ddl助手</b><br>
 * <b>@author</b> 肖乾斌
 * 
 */
public abstract class DDLHelper {

    protected Connection conn;

    protected static Logger logger = LoggerFactory.getLogger(DDLHelper.class);

    protected Map<Class<?>, String> typeStringCache = new HashMap<>();

    // 缓存所有dll helper
    private static Map<DialectType, DDLHelper> helpers = new ConcurrentHashMap<>();

    protected static final String DATETIME = "DATETIME";
    protected static final String TIMESTAMP = "TIMESTAMP";
    protected static final String DATE = "DATE";
    protected static final String VARCHAR = "VARCHAR";
    protected static final String VARCHAR2 = "VARCHAR2";
    protected static final String FLOAT = "FLOAT";
    protected static final String INTEGER = "INTEGER";
    protected static final String BIGINT = "BIGINT";
    protected static final String DOUBLE = "DOUBLE";
    protected static final String NUMBER20 = "NUMBER(20, 0)";
    protected static final String NUMBER8_5 = "NUMBER(8, 5)";
    protected static final String LONG_BLOB = "LONGBLOB";
    protected static final String BLOB = "BLOB";
    protected static final String IMAGE = "IMAGE";

    public static void init() {
        helpers.put(DialectType.MYSQL, new MySQLDDLHelper());
        helpers.put(DialectType.ORACLE, new OracleDDLHelper());
        helpers.put(DialectType.SQLSERVER, new SQLServerDDLHelper());
        helpers.put(DialectType.DB2, new DB2DDLHelper());
        helpers.put(DialectType.SQLITE3, new SQLiteDDLHelper());
    }

    protected Connection getConnection() {
        return conn;
    }

    /**
     * 
     * <b>Description: 获取已经存在表</b><br>
     * @return
     * 
     */
    protected Set<String> getExistedTables() {
        try {
            return readTablesFromDB(getConnection());
        } catch (SQLException e) {
            throw new RabbitDDLException(e);
        }
    }

    public static Set<String> readTablesFromDB(Connection connection) throws SQLException {
        ResultSet tables = null;
        try {
            tables = connection.getMetaData().getTables(null, null, null, null);
            HashSet<String> existsTables = new HashSet<>();
            while (tables.next()) {
                if ("TABLE".equalsIgnoreCase(tables.getString("TABLE_TYPE"))) {
                    existsTables.add(tables.getString("TABLE_NAME"));
                }
            }
            return existsTables;
        } finally {
            if (null != tables) {
                tables.close();
            }
        }
    }

    /**
     * 
     * <b>Description: 删除指定表</b><br>
     * @param entities 实体class的集合
     * 
     */
    protected abstract void dropTables(HashSet<String> entities);

    /**
     * 
     * <b>Description: 创建表</b><br>
     * @param entities 实体class的集合
     * 
     */
    protected void createTables(HashSet<String> entities) {
        createEntityTables(entities);
        createJoinTables(entities);
    }

    /**
     * 
     * <b>Description: 实体实体表</b><br>
     * @param entities
     * 
     */

    protected abstract void createEntityTables(Set<String> entities);
    
    /**
     * <b>@description 建表时的注释sql  </b>
     * @return
     */
    protected abstract List<StringBuilder> getCommentSqls();

    /**
     * 
     * <b>Description: 创建关联表</b><br>
     * @param entities
     * 
     */
    protected abstract void createJoinTables(HashSet<String> entities);

    /**
     * 
     * <b>Description: 更新表</b><br>
     * @param entities 实体class的集合
     * 
     */
    protected void updateTables(HashSet<String> entities) {
        // 该表已经有的旧表
        alterTable(entities);
        // 新加表
        createEntityTables(getEntityTables2Create(entities, getExistedTables()));
        // 新增中间表
        addJoinTables(entities);
    }

    /**
     * 
     * <b>Description: 查找需要新建的实体表</b><br>
     * @param entities
     * @param existedTables
     * @return
     * 
     */
    private Set<String> getEntityTables2Create(Set<String> entities,
            Set<String> existedTables) {
        Set<String> table2Create = new HashSet<>();
        for (String name : entities) {
            Class<?> clz = getClassByName(name);
            Entity entity = clz.getAnnotation(Entity.class);
            if (entity.ddlIgnore()) {
                continue;
            }
            if (!isTableExists(existedTables, entity.value())) {
                table2Create.add(clz.getName());
            }
        }
        return table2Create;
    }

    protected void setConnection(Connection conn) {
        this.conn = conn;
    }

    /**
     * 
     * <b>Description: 执行dll</b><br>
     * 
     * @param factory
     * 
     */
    public static void executeDDL(SessionFactory factory) {
        if (!requireDDL(factory)) {
            return;
        }
        DDLHelper ddlHelper = helpers.get(factory.getDialectType());
        Connection connection = null;
        try {
            connection = factory.getConnection();
            ddlHelper.setConnection(connection);
            String upperCase = factory.getDdl().toUpperCase();
			logger.info("DDLType[{}]: {}", factory.getDialectType(), upperCase);
            if (DDLType.CREATE.name().equalsIgnoreCase(factory.getDdl())) {
                doCreate(factory, ddlHelper);
            } else {
                doUpdate(factory, ddlHelper);
            }
        } catch (Exception e) {
            throw new RabbitDDLException(e);
        } finally {
            DMLObject.closeConnection(connection);
        }
    }

    /**
     * <b>Description 获取当前的helper</b>
     * 
     * @param factory
     * @return
     */
    public static DDLHelper getCurrentDDLHelper(SessionFactory factory) {
        return helpers.get(factory.getDialectType());
    }

    /**
     * <b>Description 新建表</b>
     * @param connection
     * @param dialectType
     * @param tableName
     * @param entityClz
     */
    public static void createTable(Connection connection, DialectType dialectType, String tableName,
            Class<?> entityClz) {
        DDLHelper ddlHelper = helpers.get(dialectType);
        try {
            ddlHelper.setConnection(connection);
            ddlHelper.createTable(entityClz, tableName);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * <b>@description 执行重建表行为 </b>
     * @param factory
     * @param helper
     */
    private static void doCreate(SessionFactory factory, DDLHelper helper) {
        helper.dropTables((HashSet<String>) factory.getEntities());
        helper.createTables((HashSet<String>) factory.getEntities());
    }

    /**
     * <b>@description  执行更新表行为</b>
     * @param factory
     * @param helper
     */
    private static void doUpdate(SessionFactory factory, DDLHelper helper) {
        helper.updateTables((HashSet<String>) factory.getEntities());
    }

    private static boolean requireDDL(SessionFactory factory) {
        return DDLType.CREATE.name().equalsIgnoreCase(factory.getDdl())
                || DDLType.UPDATE.name().equalsIgnoreCase(factory.getDdl());
    }

    /**
     * 
     * <b>Description: 获取中间表信息</b><br>
     * @param entities
     * @return
     * 
     */
    protected Map<String, List<JoinTableDescriptor>> getJoinTables(Set<String> entities) {
        Map<String, List<JoinTableDescriptor>> joinTables = new HashMap<>();
        for (String clzName : entities) {
            Class<?> clz = getClassByName(clzName);
            joinTables.putAll(getJoinTablesByClz(entities, clz));
        }
        return joinTables;
    }

    private Map<String, List<JoinTableDescriptor>> getJoinTablesByClz(Set<String> entities, Class<?> clz) {
        Class<?> c = clz;
        Map<String, List<JoinTableDescriptor>> joinTables = new HashMap<>();
        while (!c.equals(Object.class)) {
            for (Field field : clz.getDeclaredFields()) {
                ManyToMany m2m = field.getAnnotation(ManyToMany.class);
                if (null == m2m || isEntity(entities, m2m)) {
                    continue;
                }
                List<JoinTableDescriptor> des = getJoinTableDescription(clz, field, m2m);
                joinTables.put(m2m.joinTable().toUpperCase(), des);
            }
            c = c.getSuperclass();
        }
        return joinTables;
    }

    protected Class<?> getClassByName(String clzName) {
        Class<?> clz;
        try {
            clz = Class.forName(clzName);
        } catch (ClassNotFoundException e) {
            throw new RabbitDDLException(e);
        }
        return clz;
    }

    private List<JoinTableDescriptor> getJoinTableDescription(Class<?> clz, Field f, ManyToMany m2m) {
        List<JoinTableDescriptor> descriptors = new ArrayList<>();
        if (!m2m.policy().equals(Policy.NONE)) {
            descriptors.add(new JoinTableDescriptor(m2m.policy().equals(Policy.UUID) ? String.class
                            : Long.class, m2m.id(), m2m.policy(), m2m.policy()
                            .equals(Policy.UUID) ? 36 : 0));
        }
        if ("".equals(m2m.masterFieldName().trim())) {
        	descriptors.add(new JoinTableDescriptor(MetaData.getPrimaryKeyFieldMeta(clz).getField().getType(), 
            		m2m.joinColumn(), MetaData.getPrimaryKeyFieldMeta(clz).getColumn().length()));
        } else {
        	// 自定义关联字段
        	FieldMetaData cfm = MetaData.getCachedFieldsMeta(clz, m2m.masterFieldName().trim());
			descriptors.add(new JoinTableDescriptor(cfm.getField().getType(), m2m.joinColumn(), cfm.getColumn().length()));
        }
        
        ParameterizedType pt = (ParameterizedType) f.getGenericType();
        if ("".equals(m2m.slaveFieldName().trim())) {
        	descriptors.add(new JoinTableDescriptor(MetaData.getPrimaryKeyFieldMeta(
                    (Class<?>) pt.getActualTypeArguments()[0]).getField().getType(), 
            		m2m.reverseJoinColumn(), 
            		MetaData.getPrimaryKeyFieldMeta((Class<?>) pt.getActualTypeArguments()[0]).getColumn().length()));
        } else {
        	// 自定义关联字段
        	FieldMetaData cfm = MetaData.getCachedFieldsMeta((Class<?>) pt.getActualTypeArguments()[0], m2m.slaveFieldName().trim());
			descriptors.add(new JoinTableDescriptor(cfm.getField().getType(), m2m.reverseJoinColumn(), cfm.getColumn().length()));
        }
        
        if (!StringUtils.isEmpty(m2m.filterColumn())) {
            descriptors.add(new JoinTableDescriptor(String.class, m2m.filterColumn(), 50));
        }
        return descriptors;
    }

    private boolean isEntity(Set<String> entities, ManyToMany m2m) {
        for (String clzName : entities) {
            String tbn;
            try {
                tbn = Class.forName(clzName).getAnnotation(Entity.class).value();
            } catch (ClassNotFoundException e) {
                throw new RabbitDDLException(e);
            }
            if (tbn.equalsIgnoreCase(m2m.joinTable())) {
                return true;
            }
        }
        return false;
    }

    protected abstract String getAutoIncrement();

    /**
     * 
     * <b>Description: 生成添加列的关键字</b><br>
     * 
     * @return
     * 
     */
    protected abstract String getAddColumnKeywords();

    protected void closeStmt(Statement stmt) {
        if (null != stmt) {
            try {
                stmt.clearBatch();
                stmt.close();
            } catch (SQLException e) {
                throw new RabbitDDLException(e);
            }
        }
    }

    /**
     * 
     * <b>Description: 创建中间表sql</b><br>
     * 
     * @param tb
     * @param list
     * @return
     * 
     */
    protected StringBuilder createJoinTableSql(String tb,
            List<JoinTableDescriptor> list) {
        StringBuilder sql = new StringBuilder("CREATE TABLE "
                + tb.toUpperCase() + "(");
        String pkName = "";
        for (JoinTableDescriptor jtd : list) {
            sql.append(getColumnName(jtd.getColumnName()) + " ");
            sql.append(getSqlTypeByJavaType(jtd.getType(),
                    jtd.getColumnLength()));
            if (null != jtd.getPolicy()) {
                if (jtd.getPolicy().equals(Policy.AUTOINCREMENT)) {
                    sql.append(getAutoIncrement() + ", ");
                } else {
                    sql.append(" NOT NULL, ");
                }
                pkName = jtd.getColumnName();
            } else {
                sql.append(", ");
            }
        }
        if (!"".equals(pkName)) {
            sql.append("PRIMARY KEY(" + getColumnName(pkName) + "),");
        }
        sql.deleteCharAt(sql.lastIndexOf(","));
        sql.append(")");
        return sql;
    }

    private String getTypeString(Class<?> type) {
        if (!typeStringCache.containsKey(type)) {
            throw new RabbitDDLException("unsupported java type["
                    + type.getName() + "] is found!");
        }
        return typeStringCache.get(type);
    }

    protected String getSqlTypeByJavaType(Class<?> type, int length) {
        if (Enum.class.isAssignableFrom(type)) {
        	return getTypeString(String.class) + "(" + length + ")";
        }
        if (type.equals(String.class)) {
            return getTypeString(type) + "(" + length + ")";
        }
        return getTypeString(type);
    }

    protected boolean isTableExists(Set<String> existsTables, String table) {
        for (String tb : existsTables) {
            if (tb.equalsIgnoreCase(table)) {
                return true;
            }
        }
        return false;
    }

    protected void addJoinTables(Set<String> entities) {
        List<StringBuilder> jts = new ArrayList<>();
        Map<String, List<JoinTableDescriptor>> joinTables = getJoinTables(entities);
        for (Entry<String, List<JoinTableDescriptor>> entry : joinTables
                .entrySet()) {
            if (!isTableExists(getExistedTables(), entry.getKey())) {
                jts.add(createJoinTableSql(entry.getKey(), entry.getValue()));
            }
        }
        if (!jts.isEmpty()) {
            executeSQL(jts);
        }
    }

    protected void alterTable(HashSet<String> entities) {
        List<StringBuilder> sql = createAlterSqls(entities, getExistedTables());
        sql.addAll(getCommentSqls());
        if (!sql.isEmpty()) {
            executeSQL(sql);
        }
    }

    /**
     * 
     * <b>Description: 生成改变表的sql</b><br>
     * @param entities
     * @param existedTables
     * 
     */
    protected List<StringBuilder> createAlterSqls(Set<String> entities, Set<String> existedTables) {
        List<StringBuilder> sqls = new ArrayList<>();
        for (String name : entities) {
            Class<?> clz = getClassByName(name);
            Entity entity = clz.getAnnotation(Entity.class);
            if (entity.ddlIgnore()
                    || !isTableExists(existedTables, entity.value())) {
                continue;
            }
            try {
                sqls.addAll(generateAlterSql(entity.value(), clz));
            } catch (SQLException e) {
                throw new RabbitDDLException(e);
            }
        }
        return sqls;
    }

    protected List<StringBuilder> generateAlterSql(String tableName,
            Class<?> entityClz) throws SQLException {
        ResultSet columns = getConnection().getMetaData().getColumns(null,
                null, tableName, null);
        List<String> cols = new ArrayList<>();
        while (columns.next()) {
            cols.add(columns.getString("COLUMN_NAME").toUpperCase());
        }
        List<StringBuilder> sqls = new ArrayList<>();
        for (FieldMetaData fmd : MetaData.getCachedFieldsMetas(entityClz).values()) {
			if (fmd.getColumn().dynamic()
					|| cols.contains(fmd.getColumn().value().toUpperCase())) {
				continue;
			}
            StringBuilder sql = new StringBuilder();
            sql.append("ALTER TABLE " + tableName + getAddColumnKeywords()
                    + getColumnName(fmd.getColumn()).toUpperCase() + " ");
            if (fmd.isForeignKey()) {
                sql.append(getSqlTypeByJavaType(
                        fmd.getForeignField().getType(), fmd.getForeignField()
                                .getAnnotation(Column.class).length()));
            } else if (fmd.isPrimaryKey()) {
                sql.append(getSqlTypeByJavaType(fmd.getField().getType(), fmd
                        .getColumn().length()));
                sql.append(createSqlByPolicy(fmd.getPrimaryKey().policy()));
                sql.append(" NOT NULL PRIMARY KEY");
            } else {
                sql.append(getSqlTypeByJavaType(fmd.getField().getType(), fmd
                        .getColumn().length()));
            }
            generateComment(sql, fmd.getColumn().comment(), tableName, fmd.getColumn().value());
            sqls.add(sql);
        }
        return sqls;
    }

	/**
	 * <b>@description  生成注释部分的sql </b>
	 * @param sql
	 * @param comment		注释
	 * @param tableName		表名
	 * @param columnName	字段名
	 */
	protected abstract void generateComment(StringBuilder sql, String comment, String tableName, String columnName);

    /**
     * 
     * <b>Description: 运行改变表的sql</b><br>
     * @param sqls
     * 
     */
    protected void executeSQL(List<StringBuilder> sqls) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            for (StringBuilder sql : sqls) {
                String upperCase = SQLFormater.format(sql.toString().trim())
                        .toUpperCase();
				logger.info("{}", upperCase);
                stmt.addBatch(sql.toString());
            }
            stmt.executeBatch();
            stmt.clearBatch();
        } catch (SQLException e) {
            throw new RabbitDDLException(e);
        } finally {
            closeStmt(stmt);
        }
    }

    protected StringBuilder createSqlByClass(String className) {
        Class<?> clz = getClassByName(className);
        Entity entity = clz.getAnnotation(Entity.class);
        if (entity.ddlIgnore()) {
            return null;
        }
        String tableName = entity.value();
        return createTableSQL(clz, tableName);
    }

    /**
     * <b>Description 创建表</b>
     * @param clz
     * @param tableName
     */
    private void createTable(Class<?> clz, String tableName) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            StringBuilder sql = createTableSQL(clz, tableName);
            String upperCase = SQLFormater.format(sql.toString()).toUpperCase();
			logger.info("{}", upperCase);
            stmt.execute(sql.toString());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        } finally {
            closeStmt(stmt);
        }
    }

    /**
     * <b>Description 生成创建分区表的sql</b>
     * @param clz
     * @param tableName
     * @return
     */
    protected StringBuilder createTableSQL(Class<?> clz, String tableName) {
    	Collection<FieldMetaData> fmds = MetaData.getCachedFieldsMetas(clz).values();
        StringBuilder sql = new StringBuilder("CREATE TABLE "
                + tableName.toUpperCase() + "(");
        String pkName = appendFieldsSQLPiece(clz, fmds, sql);
        sql.append("PRIMARY KEY (" + pkName + "))");
        return sql;
    }

    protected String appendFieldsSQLPiece(Class<?> clz,
    		Collection<FieldMetaData> fmds, StringBuilder sql) {
        FieldMetaData pkm = null;
        for (FieldMetaData fmd : fmds) {
        	if (fmd.getColumn().dynamic()) {
        		continue;
        	}
            sql.append(createFieldSqlByMeta(fmd, clz.getAnnotation(Entity.class).value()));
            if (fmd.isPrimaryKey()) {
                pkm = fmd;
            }
        }
        if (null == pkm) {
            throw new RabbitDDLException("no @PrimaryKey was found in class["
                    + clz.getName() + "]!");
        }
        return getColumnName(pkm.getColumn()).toUpperCase();
    }

    /**
     * 
     * <b>Description: 根据字段信息创建sql</b><br>
     * @param fmd
     * @param tableName 表名
     * 
     */
    protected StringBuilder createFieldSqlByMeta(FieldMetaData fmd, String tableName) {
        StringBuilder sql = new StringBuilder();
        sql.append(getColumnName(fmd.getColumn()).toUpperCase() + " ");
        if (fmd.isForeignKey()) {
            createForeignKeyColumn(fmd, sql);
        } else {
            sql.append(getSqlTypeByJavaType(fmd.getField().getType(), fmd
                    .getColumn().length()));
            if (isPrimaryKey(fmd)) {
                sql.append(" NOT NULL ");
                sql.append(createSqlByPolicy(fmd.getPrimaryKey().policy()));
            }
        }
        generateComment(sql, fmd.getColumn().comment(), tableName, fmd.getColumn().value());
        sql.append(",");
        return sql;
    }

	private void createForeignKeyColumn(FieldMetaData fmd, StringBuilder sql) {
		if ("".equals(fmd.getColumn().joinFieldName().trim())) {
			FieldMetaData pk = MetaData.getCachedFieldsMeta(fmd.getField().getType(), fmd.getForeignField().getName());
			sql.append(getSqlTypeByJavaType(pk.getField().getType(), pk.getColumn().length()));
		} else {
			FieldMetaData cfm = MetaData.getCachedFieldsMeta(fmd.getField().getType(), fmd.getColumn().joinFieldName().trim());
			sql.append(getSqlTypeByJavaType(cfm.getField().getType(), cfm.getColumn().length()));
		}
	}

    /**
     * <b>Description  根据策略添加策略sql片段</b>
     * @param policy
     */
    protected String createSqlByPolicy(Policy policy) {
        if (Policy.AUTOINCREMENT.equals(policy)) {
            return getAutoIncrement();
        }
        return "";
    }

    private boolean isPrimaryKey(FieldMetaData fmd) {
        return null != fmd.getPrimaryKey();
    }

    protected String getColumnName(String realName) {
        return realName;
    }

    public abstract String getColumnName(Column column);

    /**
     * 
     * <b>Description: 创建删除表的sql </b><br>
     * @param name
     * @return
     * 
     */
    protected String createDropSqlByClass(String name) {
        Entity entity;
        try {
            entity = Class.forName(name).getAnnotation(Entity.class);
        } catch (ClassNotFoundException e) {
            throw new RabbitDDLException(e);
        }
        if (entity.ddlIgnore()) {
            return null;
        }
        String table = entity.value();
        if (!isTableExists(getExistedTables(), table)) {
            return null;
        }
        return "DROP TABLE " + table;
    }
}
