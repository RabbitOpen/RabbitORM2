package rabbit.open.dtx.common.nio.exception;

/**
 * 客户端关闭异常
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class ClientClosedException extends RpcException {

    public ClientClosedException(String msg) {
        super(msg);
    }
}
