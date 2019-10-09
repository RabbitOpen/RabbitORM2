package rabbit.open.orm.core.dialect.page;

public class PageInfo {

	private int pageSize;
	
	private int pageIndex;

	public PageInfo(int pageIndex, int pageSize) {
		setPageIndex(pageIndex);
		setPageSize(pageSize);
	}

	public int getPageSize() {
		return pageSize;
	}

	public void setPageSize(int pageSize) {
		this.pageSize = pageSize;
	}

	public int getPageIndex() {
		return pageIndex;
	}

	public void setPageIndex(int pageIndex) {
		this.pageIndex = pageIndex;
	}
	
	
}
