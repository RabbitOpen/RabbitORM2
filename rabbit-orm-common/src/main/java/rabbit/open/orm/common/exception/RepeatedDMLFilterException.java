package rabbit.open.orm.common.exception;

/**
 * 
 * <b>@description addDMLFilter一个class两次以上 </b>
 */
@SuppressWarnings("serial")
public class RepeatedDMLFilterException extends RabbitDMLException {

	public RepeatedDMLFilterException(Class<?> clz) {
		super("[" + clz.getName() + "] can't be added more than once by addFilter(DMLFilter)");
	}

}
