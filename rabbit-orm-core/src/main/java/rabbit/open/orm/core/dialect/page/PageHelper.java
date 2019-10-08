package rabbit.open.orm.core.dialect.page;

/**
 * <b>@description SqlQuery分页插件 </b>
 */
public abstract class PageHelper {

	private static ThreadLocal<PageInfo> context = new ThreadLocal<>();
	
	private PageHelper() {}

	/**
	 * <b>@description 分页 </b>
	 * @param pageIndex 页码，从0开始
	 * @param pageSize
	 */
	public static void page(int pageIndex, int pageSize) {
		context.set(new PageInfo(pageIndex, pageSize));
	}
	
	/**
	 * <b>@description 读取分页信息 </b>
	 * @return
	 */
	public static PageInfo getPageInfo() {
		PageInfo pageInfo = context.get();
		context.remove();
		return pageInfo;
	}

	/**
	 * <b>@description 判断是否有分页参数 </b>
	 */
	public static boolean isPaged() {
		return null != context.get();
	}
}
