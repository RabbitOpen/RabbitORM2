package rabbit.open.dtx.common.exception;

/**
 * 获取rpc连接超时异常
 * @author xiaoqianbin
 * @date 2019/12/16
 **/
@SuppressWarnings("serial")
public class GetConnectionTimeoutException extends DtxException {

    public GetConnectionTimeoutException(long timeoutMilliSeconds) {
        super(String.format("get connection timeout for %s ms", timeoutMilliSeconds));
    }
}
