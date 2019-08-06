package rabbit.open.orm.common.exception;

/**
 * <b>Description: 错误的排序请求</b>. <b>@author</b> 肖乾斌
 * 
 */
@SuppressWarnings("serial")
public class OrderAssociationException extends RuntimeException {

	public OrderAssociationException(String tableName) {
		super("table[" + tableName + "] is not associcated with this query!");
	}

}
