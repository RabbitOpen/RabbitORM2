package rabbit.open.orm.exception;

/**
 * <b>Description 空别名异常 </b>. 
 */
@SuppressWarnings("serial")
public class EmptyAliasException extends RuntimeException{
    
    public EmptyAliasException(String queryName) {
        super("alias of query[" + queryName + "] is empty");
    }

}
