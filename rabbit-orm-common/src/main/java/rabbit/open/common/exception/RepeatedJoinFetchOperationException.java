package rabbit.open.common.exception;

/**
 * <b>Description 重复的join fetch 操作</b>.
 */
@SuppressWarnings("serial")
public class RepeatedJoinFetchOperationException extends RabbitDMLException {

	public RepeatedJoinFetchOperationException(Class<?> fetch, Class<?> t1,
			Class<?> t2) {
		super("repeated join fetch operation of [" + fetch.getName() + "] on ["
				+ t1.getName() + "] and [" + t2.getName() + "]");
	}

}
