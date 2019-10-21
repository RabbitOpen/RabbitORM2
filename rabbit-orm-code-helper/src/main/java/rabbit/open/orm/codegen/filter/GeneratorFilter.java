package rabbit.open.orm.codegen.filter;

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

	/**
	 * 将表名转成类名
	 * @param expectedName	按驼峰命名规则转换出来的名字名
	 * @param nameInDB		数据库中原始的名称
	 * @param type			名字类型
	 * @return				你希望的名字
	 */
	public String onNameChanged(String expectedName, String nameInDB, NameType type);
}
