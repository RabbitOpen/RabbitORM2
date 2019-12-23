package rabbit.open.dtx.common.nio.exception;

import rabbit.open.dtx.common.nio.client.annotation.Isolation;

/**
 * 隔离级别异常，Isolation.READ_COMMITTED必须在事务方法中使用
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
@SuppressWarnings("serial")
public class IsolationException extends RpcException {

    public IsolationException() {
        super(String.format("%s must be used in transactional mode", Isolation.READ_COMMITTED));
    }
}
