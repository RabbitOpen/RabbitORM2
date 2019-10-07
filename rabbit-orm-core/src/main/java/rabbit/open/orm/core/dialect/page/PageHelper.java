package rabbit.open.orm.core.dialect.page;

/**
 * <b>@description SqlQuery分页插件 </b>
 */
public abstract class PageHelper {

	private static ThreadLocal<PageInfo> context = new ThreadLocal<>();

	/**
	 * <b>@description 分页 </b>
	 * @param pageIndex 页码，从0开始
	 * @param pageSize
	 */
	public static void page(int pageIndex, int pageSize) {
		context.set(new PageInfo(pageIndex, pageSize));
	}

	/**
	 * <b>@description 清除分页信息 </b>
	 */
	public static void clear() {
		context.remove();
	}

	/**
	 * <b>@description 判断是否有分页参数 </b>
	 */
	public static boolean isPaged() {
		return null != context.get();
	}
}
