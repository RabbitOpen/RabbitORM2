package rabbit.open.dtx.common.exception;

/**
 * DTX异常类
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@SuppressWarnings("serial")
public class DistributedTransactionException extends DtxException {

    public DistributedTransactionException(String message) {
        super(message);
    }

    public DistributedTransactionException(Throwable cause) {
        super(cause);
    }
}
