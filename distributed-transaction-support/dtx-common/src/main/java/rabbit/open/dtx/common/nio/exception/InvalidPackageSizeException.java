package rabbit.open.dtx.common.nio.exception;

/**
 * 包长度异常
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
@SuppressWarnings("serial")
public class InvalidPackageSizeException extends DtxException {

    public InvalidPackageSizeException(int length, int maxSize) {
        super(String.format("data size is invalid!, max length is [{%d}], actual length [{%d}]", maxSize, length));
    }
}
