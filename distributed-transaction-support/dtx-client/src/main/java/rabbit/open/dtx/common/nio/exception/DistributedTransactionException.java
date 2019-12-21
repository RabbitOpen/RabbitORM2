package rabbit.open.dtx.common.nio.exception;

/**
 * DTX异常基类
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@SuppressWarnings("serial")
public class DistributedTransactionException extends RpcException {

    public DistributedTransactionException(String message) {
        super(message);
    }

    public DistributedTransactionException(Throwable cause) {
        super(cause);
    }
}
