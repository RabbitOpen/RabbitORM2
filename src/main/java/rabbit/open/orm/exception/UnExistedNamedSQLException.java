package rabbit.open.orm.exception;

/**
 * <b>Description 不存在的命名查询 </b> 
 */
@SuppressWarnings("serial")
public class UnExistedNamedSQLException extends RuntimeException{
    
    public UnExistedNamedSQLException(String name) {
        super("no named sql[" + name + "] is found");
    }

}
