package rabbit.open.orm.common.exception;

/**
 * 
 * <b>@description 使用冲突 </b>
 */
@SuppressWarnings("serial")
public class ConflictFilterException extends RabbitDMLException {

	public ConflictFilterException(Class<?> clz) {
		super("[" + clz.getName() + "] can't be both used by addFilter/addJoinFilter and addDMLFilter");
	}

}
