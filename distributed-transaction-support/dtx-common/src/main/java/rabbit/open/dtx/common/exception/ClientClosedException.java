package rabbit.open.dtx.common.exception;

/**
 * 客户端关闭异常
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
@SuppressWarnings("serial")
public class ClientClosedException extends DtxException {

    public ClientClosedException(String msg) {
        super(msg);
    }
}
