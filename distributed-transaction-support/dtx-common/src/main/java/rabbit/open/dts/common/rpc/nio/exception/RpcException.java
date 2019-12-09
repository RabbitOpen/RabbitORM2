package rabbit.open.dts.common.rpc.nio.exception;

/**
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
@SuppressWarnings("serial")
public class RpcException extends RuntimeException {

    public RpcException(String msg) {
        super(msg);
    }

}
