package rabbit.open.dtx.client.exception;

/**
 * DTX异常基类
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class DistributedTransactionException extends RuntimeException {

    public DistributedTransactionException(String message) {
        super(message);
    }

    public DistributedTransactionException(Throwable cause) {
        super(cause);
    }
}
