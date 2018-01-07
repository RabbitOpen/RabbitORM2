package rabbit.open.orm.exception;

/**
 * <b>Description 没有关于clz的命名查询 </b> 
 */
@SuppressWarnings("serial")
public class NoNamedSQLDefinedException extends RuntimeException{
    
    public NoNamedSQLDefinedException(Class<?> clz) {
        super("no named sql is defined for class[" + clz.getName() + "]");
    }

}
