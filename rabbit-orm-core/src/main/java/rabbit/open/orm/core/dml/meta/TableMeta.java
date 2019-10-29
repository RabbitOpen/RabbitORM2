package rabbit.open.orm.core.dml.meta;

import javax.sql.DataSource;

/**
 * <b>@description 表的元信息 </b>
 */
public class TableMeta {
	
	// 表名
	private String tableName;
	
	// 数据源
	private DataSource dataSource;

	public TableMeta(String tableName, DataSource dataSource) {
		super();
		this.tableName = tableName;
		this.dataSource = dataSource;
	}

	public String getTableName() {
		return tableName;
	}

	public DataSource getDataSource() {
		return dataSource;
	}
	
	
}
