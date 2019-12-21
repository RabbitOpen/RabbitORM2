package rabbit.open.dtx.common.nio.exception;

/**
 * rpc异常基类
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
@SuppressWarnings("serial")
public class RpcException extends RuntimeException {

    public RpcException(String msg) {
        super(msg);
    }

    public RpcException(Throwable cause) {
        super(cause);
    }
}
