package rabbit.open.orm.exception;

/**
 * <b>Description  重复的join fetch 操作</b>. 
 */
@SuppressWarnings("serial")
public class RepeatedFetchOperationException extends RabbitDMLException{

    public RepeatedFetchOperationException(Class<?> fetch, Class<?> t1, Class<?> t2) {
        super("repeated fetch operation of [" + fetch.getName() + "] between [" 
                + t1.getName() + "] and [" + t2.getName() + "]");
    }

}
