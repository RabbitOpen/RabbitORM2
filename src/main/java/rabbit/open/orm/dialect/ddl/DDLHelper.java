package rabbit.open.orm.dialect.ddl;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import rabbit.open.orm.annotation.Column;
import rabbit.open.orm.annotation.Entity;
import rabbit.open.orm.annotation.ManyToMany;
import rabbit.open.orm.ddl.DDLType;
import rabbit.open.orm.ddl.JoinTableDescriptor;
import rabbit.open.orm.ddl.PackageScanner;
import rabbit.open.orm.dialect.ddl.impl.DB2DDLHelper;
import rabbit.open.orm.dialect.ddl.impl.MySQLDDLHelper;
import rabbit.open.orm.dialect.ddl.impl.OracleDDLHelper;
import rabbit.open.orm.dialect.ddl.impl.SQLServerDDLHelper;
import rabbit.open.orm.dialect.dml.DialectType;
import rabbit.open.orm.dml.DMLAdapter;
import rabbit.open.orm.dml.meta.FieldMetaData;
import rabbit.open.orm.dml.meta.MetaData;
import rabbit.open.orm.dml.policy.Policy;
import rabbit.open.orm.dml.util.SQLFormater;
import rabbit.open.orm.exception.RabbitDDLException;
import rabbit.open.orm.pool.SessionFactory;

/**
 * <b>Description: 	ddl助手</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
public abstract class DDLHelper {

    protected Connection conn;
    
	protected static Logger logger = Logger.getLogger(DDLHelper.class);
	
	//缓存所有dllhelper
    private static Map<DialectType, DDLHelper> helpers = new ConcurrentHashMap<>();
    
    public static void init(){
        helpers.put(DialectType.MYSQL, new MySQLDDLHelper());
        helpers.put(DialectType.ORACLE, new OracleDDLHelper());
        helpers.put(DialectType.SQLSERVER, new SQLServerDDLHelper());
        helpers.put(DialectType.DB2, new DB2DDLHelper());
    }
    
	protected Connection getConnection(){
		return conn;
	}
	
	/**
	 * 
	 * <b>Description:	获取已经存在表</b><br>
	 * @return
	 * 
	 */
	protected HashSet<String> getExistedTables() {
		try {
			return readTablesFromDB();
		} catch (SQLException e) {
			throw new RabbitDDLException(e);
		}
	}

	protected HashSet<String> readTablesFromDB() throws SQLException {
		ResultSet tables = null;
		try{
		    tables = getConnection().getMetaData().getTables(null, null, null, null);
	        HashSet<String> existsTables = new HashSet<>();
	        while(tables.next()){
	            if("TABLE".equalsIgnoreCase(tables.getString("TABLE_TYPE"))){
	                existsTables.add(tables.getString("TABLE_NAME"));
	            }
	        }
	        return existsTables;
		} finally {
		    if(null != tables){
		        tables.close();
		    }
		}
	}
	
	/**
	 * 
	 * <b>Description:	删除指定表</b><br>
	 * @param entities	实体class的集合
	 * 
	 */
	protected abstract void dropTables(HashSet<String> entities);

	/**
	 * 
	 * <b>Description:	创建表</b><br>
	 * @param entities	实体class的集合
	 * 
	 */
	protected void createTables(HashSet<String> entities){
	    createEntityTables(entities);
        createJoinTables(entities);
	}
	
   /**
     * 
     * <b>Description:  实体表</b><br>
     * @param entities  
     * 
     */

	protected abstract void createEntityTables(HashSet<String> entities);
	
	/**
	 * 
	 * <b>Description:  创建实体表</b><br>.
	 * @param entities	
	 * 
	 */
	protected abstract void createJoinTables(HashSet<String> entities);

	/**
	 * 
	 * <b>Description:	更新表</b><br>
	 * @param entities	实体class的集合
	 * 
	 */
	protected void updateTables(HashSet<String> entities){
	  //该表已经有的旧表
        alterTable(entities);
        //新加表
        createEntityTables(getEntityTables2Create(entities, getExistedTables()));
        //新增中间表
        addJoinTables(entities);
	}
	
	
	/**
     * 
     * <b>Description:  查找需要新建的实体表</b><br>
     * @param entities
     * @param existedTables
     * @return  
     * 
     */
    private HashSet<String> getEntityTables2Create(HashSet<String> entities,
            HashSet<String> existedTables) {
        HashSet<String> table2Create = new HashSet<>();
        for(String name : entities){
            Class<?> clz = getClassByName(name);
            Entity entity = clz.getAnnotation(Entity.class);
            if(entity.ddlIgnore()){
                continue;
            }
            if(!isTableExists(existedTables, entity.value())){
                table2Create.add(clz.getName());
            }
        }
        return table2Create;
    }
    
	protected void setConnection(Connection conn){
	    this.conn = conn;
	}
	
	/**
	 * 
	 * <b>Description:	执行dll</b><br>
	 * @param factory	
	 * @param basePackages	
	 * 
	 */
	public static void executeDDL(SessionFactory factory, String basePackages){
		if(!requireDDL(factory)){
			return;
		}
		DDLHelper ddlHelper = helpers.get(factory.getDialectType());
		Connection connection = null;
		try {
			connection = factory.getConnection();
			ddlHelper.setConnection(connection);
			logger.info("DDLType[" + factory.getDialectType().name() + "]: " + factory.getDdl().toUpperCase());
			if(DDLType.CREATE.name().equalsIgnoreCase(factory.getDdl())){
				doCreate(ddlHelper, basePackages);
			}else{
				doUpdate(ddlHelper, basePackages);
			}
		} catch (Exception e) {
			throw new RabbitDDLException(e);
		} finally {
		    DMLAdapter.closeConnection(connection);
		}
	}
	
    /**
     * <b>Description  新建分表</b>
     * @param factory
     * @param tableName
     * @param entityClz
     */
    public static void addShardingTable(SessionFactory factory,
            String tableName, Class<?> entityClz) {
        DDLHelper ddlHelper = helpers.get(factory.getDialectType());
        Connection connection = null;
        try {
            connection = factory.getConnection();
            ddlHelper.setConnection(connection);
            ddlHelper.createShardingTable(entityClz, tableName);
        } catch (Exception e) {
            throw new RabbitDDLException(e);
        } finally {
            DMLAdapter.closeConnection(connection);
        }
    }
	
	/**
	 * 
	 * <b>Description:	执行重建表行为</b><br>
	 * @param helper
	 * @param packages	
	 * 
	 */
	private static void doCreate(DDLHelper helper, String packages){
		HashSet<String> entities = (HashSet<String>) PackageScanner.filterByAnnotation(packages.split(","), Entity.class);
		helper.dropTables(entities);
		helper.createTables(entities);
	}
	
	/**
	 * 
	 * <b>Description:	执行更新表行为</b><br>
	 * @param helper
	 * @param packages	
	 * 
	 */
	private static void doUpdate(DDLHelper helper, String packages){
		HashSet<String> entities = (HashSet<String>) PackageScanner.filterByAnnotation(packages.split(","), Entity.class);
		helper.updateTables(entities);
	}

	private static boolean requireDDL(SessionFactory factory) {
		return DDLType.CREATE.name().equalsIgnoreCase(factory.getDdl()) || 
				DDLType.UPDATE.name().equalsIgnoreCase(factory.getDdl());
	}
	
	/**
	 * 
	 * <b>Description:	获取中间表信息</b><br>
	 * @param entities
	 * @return	
	 * 
	 */
	protected HashMap<String, List<JoinTableDescriptor>> getJoinTables(HashSet<String> entities) {
		HashMap<String, List<JoinTableDescriptor>> joinTables = new HashMap<>();
		for(String clzName : entities){
			Class<?> clz = getClassByName(clzName);
			while(!clz.equals(Object.class)){
				for(Field f : clz.getDeclaredFields()){
					ManyToMany m2m = f.getAnnotation(ManyToMany.class);
					if(null == m2m || isEntity(entities, m2m)){
						continue;
					}
					List<JoinTableDescriptor> des = getJoinTableDescription(clz, f, m2m);
					joinTables.put(m2m.joinTable().toUpperCase(), des);
				}
				clz = clz.getSuperclass();
			}
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

	private List<JoinTableDescriptor> getJoinTableDescription(Class<?> clz, Field f,
			ManyToMany m2m) {
		List<JoinTableDescriptor> des = new ArrayList<>();
		if(!m2m.policy().equals(Policy.NONE)){
			des.add(new JoinTableDescriptor(m2m.policy().equals(Policy.UUID) ? String.class : Long.class, 
					m2m.id(), m2m.policy(), m2m.policy().equals(Policy.UUID) ? 36 : 0));
		}
		des.add(new JoinTableDescriptor(MetaData.getPrimaryKeyField(clz).getType(), m2m.joinColumn(), 
				MetaData.getPrimaryKeyField(clz).getAnnotation(Column.class).length()));
		ParameterizedType pt = (ParameterizedType) f.getGenericType();
		des.add(new JoinTableDescriptor(MetaData.getPrimaryKeyField((Class<?>) pt.getActualTypeArguments()[0]).getType(), m2m.reverseJoinColumn(),
				MetaData.getPrimaryKeyField((Class<?>) pt.getActualTypeArguments()[0]).getAnnotation(Column.class).length()));
		return des;
	}
	
	private boolean isEntity(HashSet<String> entities, ManyToMany m2m){
		for(String clzName : entities){
			String tbn;
			try {
				tbn = Class.forName(clzName).getAnnotation(Entity.class).value();
			} catch (ClassNotFoundException e) {
				throw new RabbitDDLException(e);
			}
			if(tbn.equalsIgnoreCase(m2m.joinTable())){
				return true;
			}
		}
		return false;
	}
	
	protected abstract String getVarcharType();

	protected abstract String getDateType();
	
	protected abstract String getAutoIncrement();
	
	/**
	 * 
	 * <b>Description:	生成添加列的关键字</b><br>
	 * @return	
	 * 
	 */
	protected abstract String getAddColumnKeywords();
	
	protected void closeStmt(Statement stmt) {
        if(null != stmt){
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
     * <b>Description:  创建中间表sql</b><br>
     * @param tb
     * @param list
     * @return
     * 
     */
    protected StringBuilder createJoinTableSql(String tb, List<JoinTableDescriptor> list) {
        StringBuilder sql = new StringBuilder("CREATE TABLE " + tb.toUpperCase() + "(");
        String pkName = "";
        for(JoinTableDescriptor jtd : list){
            sql.append(getColumnName(jtd.getColumnName()) + " ");
            sql.append(getSqlTypeByJavaType(jtd.getType(), jtd.getColumnLength()));
            if(null != jtd.getPolicy()){
                if(jtd.getPolicy().equals(Policy.AUTOINCREMENT)){
                    sql.append(getAutoIncrement() + ", ");
                }else{
                    sql.append(" NOT NULL, ");
                }
                pkName = jtd.getColumnName();
            }else{
                sql.append(", ");
            }
        }
        if(!"".equals(pkName)){
            sql.append("PRIMARY KEY(" + getColumnName(pkName) + "),");
        }
        sql.deleteCharAt(sql.lastIndexOf(","));
        sql.append(")");
        return sql;
    }
    
    public abstract String getNumberType();

    public abstract String getFloatType();

    public abstract String getDoubleType();

    public abstract String getBigDecimalType();
    
    protected String getSqlTypeByJavaType(Class<?> type, int length) {
        if(type.equals(Date.class)){
            return getDateType();
        }
        if(type.equals(String.class)){
            return getVarcharType() + "(" + length + ")";
        }
        if(type.equals(Integer.class) || type.equals(Short.class) || type.equals(Long.class)){
            return getNumberType();
        }
        if(type.equals(Float.class)){
            return getFloatType();
        }
        if(type.equals(Double.class)){
            return getDoubleType();
        }
        if(type.equals(BigDecimal.class)){
            return getBigDecimalType();
        }
        throw new RabbitDDLException("unsupported java type[" + type.getName() + "] is found!");
    }
    
    protected boolean isTableExists(HashSet<String> existsTables,
            String table) {
        for(String tb : existsTables){
            if(tb.equalsIgnoreCase(table)){
                return true;
            }
        }
        return false;
    }
    
    protected void addJoinTables(HashSet<String> entities) {
        List<StringBuilder> jts = new ArrayList<>();
        HashMap<String, List<JoinTableDescriptor>> joinTables = getJoinTables(entities);
        for(Entry<String, List<JoinTableDescriptor>> entry : joinTables.entrySet()){
            if(!isTableExists(getExistedTables(), entry.getKey())){
                jts.add(createJoinTableSql(entry.getKey(), entry.getValue()));
            }
        }
        if(!jts.isEmpty()){
            executeSQL(jts);
        }
    }

    protected void alterTable(HashSet<String> entities) {
        List<StringBuilder> sql = createAlterSqls(entities, getExistedTables());
        if(!sql.isEmpty()){
            executeSQL(sql);
        }
    }
    
    /**
     * 
     * <b>Description:  生成改变表的sql</b><br>
     * @param entities
     * @param existedTables
     * 
     */
    protected List<StringBuilder> createAlterSqls(HashSet<String> entities,
            HashSet<String> existedTables) {
        List<StringBuilder> sqls = new ArrayList<>();
        for(String name : entities){
            Class<?> clz = getClassByName(name);
            Entity entity = clz.getAnnotation(Entity.class);
            if(entity.ddlIgnore() || !isTableExists(existedTables, entity.value())){
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
    
    protected List<StringBuilder> generateAlterSql(String tableName, Class<?> entityClz) throws SQLException {
        ResultSet columns = getConnection().getMetaData().getColumns(null, null, tableName, null);
        List<String> cols = new ArrayList<>();
        while(columns.next()){
            cols.add(columns.getString("COLUMN_NAME").toUpperCase());
        }
        List<StringBuilder> sqls = new ArrayList<>();
        for(FieldMetaData fmd : MetaData.getCachedFieldsMetas(entityClz)){
            StringBuilder sql = new StringBuilder();
            if(cols.contains(fmd.getColumn().value().toUpperCase().replaceAll("\"", "").replaceAll("`", ""))){
                continue;
            }
            sql.append("ALTER TABLE " + tableName + getAddColumnKeywords() + getColumnName(fmd.getColumn().value()) + " ");
            if (fmd.isForeignKey()) {
                sql.append(getSqlTypeByJavaType(fmd.getForeignField().getType(), fmd.getForeignField().getAnnotation(Column.class).length()));
            } else if(fmd.isPrimaryKey()) {
                sql.append(getSqlTypeByJavaType(fmd.getField().getType(), fmd.getColumn().length()));
                if(Policy.AUTOINCREMENT.equals(fmd.getPrimaryKey().policy())){
                    sql.append(getAutoIncrement());
                }
                sql.append(" NOT NULL PRIMARY KEY");
            } else {
                sql.append(getSqlTypeByJavaType(fmd.getField().getType(), fmd.getColumn().length()));
            }
            sqls.add(sql);
        }
        return sqls;
    }
    
    /**
     * 
     * <b>Description:  运行改变表的sql</b><br>
     * @param sqls  
     * 
     */
    protected void executeSQL(List<StringBuilder> sqls) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            for(StringBuilder sql : sqls){
                logger.info(SQLFormater.format(sql.toString().trim()).toUpperCase());
                stmt.addBatch(sql.toString());
            }
            stmt.executeBatch();
        } catch (SQLException e) {
            throw new RabbitDDLException(e);
        } finally {
            closeStmt(stmt);
        }
    }
    
    protected StringBuilder createSqlByClass(String className){
        Class<?> clz = getClassByName(className);
        Entity entity = clz.getAnnotation(Entity.class);
        if(entity.ddlIgnore()){
            return null;
        }
        String tableName = entity.value();  
        return createTableSQL(clz, tableName);
    }

    /**
     * <b>Description  创建分区表</b>
     * @param clz
     * @param tableName
     */
    private void createShardingTable(Class<?> clz, String tableName) {
        Statement stmt = null;
        try {
            stmt = conn.createStatement();
            StringBuilder sql = createTableSQL(clz, tableName);
            logger.info(SQLFormater.format(sql.toString()).toUpperCase());
            stmt.execute(sql.toString());
        } catch (Exception e) {
            logger.warn(e.getMessage());
        } finally {
            closeStmt(stmt);
        }
    }
    
    /**
     * <b>Description  生成创建分区表的sql</b>
     * @param clz
     * @param tableName
     * @return
     */
    private StringBuilder createTableSQL(Class<?> clz, String tableName) {
        List<FieldMetaData> fmds = MetaData.getCachedFieldsMetas(clz);
        StringBuilder sql = new StringBuilder("CREATE TABLE " + tableName.toUpperCase() + "(");
        FieldMetaData pkm = null;
        for(FieldMetaData fmd : fmds){
            sql.append(createFieldSqlByMeta(fmd));
            if(fmd.isPrimaryKey()){
                pkm = fmd;
            }
        }
        if(null == pkm){
            throw new RabbitDDLException("no @PrimaryKey was found in class[" + clz.getName() + "]!");
        }
        sql.append("PRIMARY KEY (" + getColumnName(pkm.getColumn().value().toUpperCase()) + "))");
        return sql;
    }
    
    /**
     * 
     * <b>Description:  根据字段信息创建sql</b><br>
     * @param fmd   
     * 
     */
    private StringBuilder createFieldSqlByMeta(FieldMetaData fmd){
        StringBuilder sql = new StringBuilder();
        sql.append(getColumnName(fmd.getColumn().value().toUpperCase()) + " ");
        if(fmd.isForeignKey()){
            FieldMetaData pk = MetaData.getCachedFieldsMeta(fmd.getField().getType(), fmd.getForeignField().getName());
            sql.append(getSqlTypeByJavaType(pk.getField().getType(), pk.getColumn().length()));
        }else{
            sql.append(getSqlTypeByJavaType(fmd.getField().getType(), fmd.getColumn().length()));
            if(null != fmd.getPrimaryKey()){
                sql.append(" NOT NULL ");
                if(Policy.AUTOINCREMENT.equals(fmd.getPrimaryKey().policy())){
                    sql.append(getAutoIncrement());
                }
            }
        }
        sql.append(",");
        return sql;
    }
    
    protected abstract String getColumnName(String realName);
    
    /**
     * 
     * <b>Description: 创建删除表的sql </b><br>.
     * @param name
     * @return	
     * 
     */
    protected String createDropSqlByClass(String name){
        Entity entity;
        try {
            entity = Class.forName(name).getAnnotation(Entity.class);
        } catch (ClassNotFoundException e) {
            throw new RabbitDDLException(e);
        }
        if(entity.ddlIgnore()){
            return null;
        }
        String table = entity.value();
        if(!isTableExists(getExistedTables(), table)){
            return null;
        }
        return "DROP TABLE " + table;
    }
}
