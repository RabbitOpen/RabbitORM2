package rabbit.open.dtx.common.nio.exception;

/**
 * dtx异常基类
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
@SuppressWarnings("serial")
public class DtxException extends RuntimeException {

    public DtxException(String msg) {
        super(msg);
    }

    public DtxException(Throwable cause) {
        super(cause);
    }
}
