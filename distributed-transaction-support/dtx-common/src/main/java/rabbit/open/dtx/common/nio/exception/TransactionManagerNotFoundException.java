package rabbit.open.dtx.common.nio.exception;

/**
 * 事务管理器对象没声明异常
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
@SuppressWarnings("serial")
public class TransactionManagerNotFoundException extends RuntimeException {

    public TransactionManagerNotFoundException(String managerName) {
        super(String.format("%s is not existed!", managerName));
    }

}
