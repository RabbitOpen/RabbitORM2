package rabbit.open.orm.dialect.dml;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import rabbit.open.orm.dml.DB2DeleteGenerator;
import rabbit.open.orm.dml.Delete;
import rabbit.open.orm.dml.MySQLDeleteGenerator;
import rabbit.open.orm.dml.OracleDeleteGenerator;
import rabbit.open.orm.dml.SqlServerDeleteGenerator;
import rabbit.open.orm.exception.RabbitDMLException;


/**
 * <b>Description: 	删除操作方言适配器</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
public abstract class DeleteDialectAdapter {

	private static Map<DialectType, DeleteDialectAdapter> cache = new ConcurrentHashMap<>();
	
	/**
	 * 
	 * <b>Description:	构建删除sql</b><br>
	 * @param delete
	 * @return	
	 * 
	 */
	public abstract StringBuilder createDeleteSql(Delete<?> delete);
	
	/**
	 * 
	 * <b>Description:	注册删除语句方言生成器</b><br>
	 * @param dialect
	 * @param generator	
	 * 
	 */
	private static void registDialectGenerator(DialectType dialect, DeleteDialectAdapter generator){
		cache.put(dialect, generator);
	}
	
	/**
	 * 
	 * <b>Description:	根据方言获取生成器</b><br>
	 * @param dialect
	 * @return	
	 * 
	 */
	public static DeleteDialectAdapter getDialectGenerator(DialectType dialect){
		if(!cache.containsKey(dialect)){
			throw new RabbitDMLException("unkown dialect[" + dialect + "] is found!");
		}
		return cache.get(dialect);
	}
	
	public static void init() {
		registDialectGenerator(DialectType.MYSQL, new MySQLDeleteGenerator());
		registDialectGenerator(DialectType.ORACLE, new OracleDeleteGenerator());
		registDialectGenerator(DialectType.DB2, new DB2DeleteGenerator());
		registDialectGenerator(DialectType.SQLSERVER, new SqlServerDeleteGenerator());
	}
	
	
}
