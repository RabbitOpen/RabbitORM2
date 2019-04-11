package rabbit.open.orm.exception;

import rabbit.open.orm.dml.meta.MetaData;

/**
 * <b>Description: 错误的排序请求</b>. <b>@author</b> 肖乾斌
 * 
 */
@SuppressWarnings("serial")
public class OrderAssociationException extends RuntimeException {

	public OrderAssociationException(Class<?> clz) {
		super("table[" + MetaData.getTableNameByClass(clz)
				+ "] is not associcated with this query!");
	}

}
