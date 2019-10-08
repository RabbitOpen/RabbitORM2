package rabbit.open.orm.core.dialect.page;

/**
 * 分页包装
 * <b>@description  </b>
 */
public interface Pager {
	
	/**
	 * <b>@description 分页sql包装 </b>
	 * @param sql
	 * @return
	 */
	StringBuilder doPage(StringBuilder sql);

}
