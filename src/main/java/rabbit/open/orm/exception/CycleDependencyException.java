package rabbit.open.orm.exception;

/**
 * <b>Description 循环依赖异常， 依赖链路中不能出现两个相同的class </b>. 
 */
@SuppressWarnings("serial")
public class CycleDependencyException extends RuntimeException{
    
    public CycleDependencyException(Class<?> clz) {
        super("cycle dependency[" + clz.getName() + "] is not allowed!");
    }

}
