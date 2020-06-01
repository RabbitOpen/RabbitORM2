package rabbit.open.orm.common.exception;

/**
 * 不支持的操作类型
 * @author xiaoqianbin
 * @date 2020/6/1
 **/
@SuppressWarnings("serial")
public class UnSupportedOperationException extends RabbitDMLException {

    public UnSupportedOperationException(String message) {
        super(message);
    }
}
