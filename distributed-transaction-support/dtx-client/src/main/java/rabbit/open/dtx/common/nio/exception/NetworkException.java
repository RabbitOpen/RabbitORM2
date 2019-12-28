package rabbit.open.dtx.common.nio.exception;

/**
 * 网络连接异常
 * @author xiaoqianbin
 * @date 2019/12/8
 **/
@SuppressWarnings("serial")
public class NetworkException extends RpcException {

    public NetworkException(Exception e) {
        super(e.getMessage());
    }
}