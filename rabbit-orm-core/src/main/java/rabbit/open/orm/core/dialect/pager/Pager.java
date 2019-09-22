package rabbit.open.orm.core.dialect.pager;

/**
 * 分页包装
 * <b>@description  </b>
 */
public interface Pager {
	
	/**
	 * 分页sql包装
	 * <b>@description  </b>
	 * @param sql
	 * @return
	 */
	StringBuilder doPage(StringBuilder sql);

}
