package rabbit.open.dtx.common.exception;

/**
 * 网络连接异常
 * @author xiaoqianbin
 * @date 2019/12/8
 **/
@SuppressWarnings("serial")
public class NetworkException extends DtxException {

    public NetworkException(Exception e) {
        super(e.getMessage());
    }
}
