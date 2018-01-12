package rabbit.open.orm.exception;

/**
 * <b>Description 循环fetch， 自己取自己 </b>. 
 */
@SuppressWarnings("serial")
public class CycleFetchException extends RuntimeException{
    
    public CycleFetchException(Class<?> clz) {
        super("cycle fetch operation for class[" + clz.getName() + "]");
    }

}
