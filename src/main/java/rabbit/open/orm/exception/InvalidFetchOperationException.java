package rabbit.open.orm.exception;

@SuppressWarnings("serial")
public class InvalidFetchOperationException extends RabbitDMLException{

    public InvalidFetchOperationException(String message) {
        super(message);
    }

    public InvalidFetchOperationException(Class<?> fetch, Class<?> t1, Class<?> t2) {
        super("repeated fetch operation of [" + fetch.getName() + "] between [" 
                + t1.getName() + "] and [" + t2.getName() + "]");
    }
}
