package rabbit.open.codegen.filter;

/**
 * <b>@description 表过滤器 </b>
 */
public interface GeneratorFilter {

	/**
	 * <b>@description 表筛选器 </b>
	 * @param 	tableName  	表名
	 * @return 	true		表示需要生成该表的代码
	 */
	public boolean filterTable(String tableName);

	/**
	 * <b>@description 字段筛选器 </b>
	 * @param columnName
	 * @return	true表示需要生成该字段的映射
	 */
	public boolean filterColumn(String columnName);
}
