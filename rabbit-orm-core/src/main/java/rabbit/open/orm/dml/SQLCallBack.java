package rabbit.open.orm.dml;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * <b>Description: 	sql回调</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
public interface SQLCallBack<T> {

	/**
	 * 
	 * <b>Description:	执行sql</b><br>
	 * @param stmt
	 * @return	
	 * 
	 */
	public T execute(PreparedStatement stmt) throws SQLException;
}
