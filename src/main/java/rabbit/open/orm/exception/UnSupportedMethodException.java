package rabbit.open.orm.exception;

/**
 * <b>Description: 	不支持该方法</b><br>
 * <b>@author</b>	肖乾斌
 * 
 */
@SuppressWarnings("serial")
public class UnSupportedMethodException extends RuntimeException{

	public UnSupportedMethodException(String method) {
		super("method[" + method + "] is not supported");
	}

	
}
