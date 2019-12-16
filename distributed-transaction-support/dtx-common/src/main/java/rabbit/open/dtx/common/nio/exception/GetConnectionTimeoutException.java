package rabbit.open.dtx.common.nio.exception;

/**
 * 获取rpc连接超时异常
 * @author xiaoqianbin
 * @date 2019/12/16
 **/
@SuppressWarnings("serial")
public class GetConnectionTimeoutException extends RpcException {

    public GetConnectionTimeoutException(long timeoutMilliSeconds) {
        super(String.format("get connection timeout for %s ms", timeoutMilliSeconds));
    }
}
