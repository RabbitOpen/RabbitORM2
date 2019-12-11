package rabbit.open.dtx.common.nio.exception;

/**
 * 超时异常
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
@SuppressWarnings("serial")
public class TimeoutException extends RuntimeException {

    private final long timeoutSeconds;

    public TimeoutException(long seconds) {
        super(String.format("timeout %ss", seconds));
        this.timeoutSeconds = seconds;
    }

    public long getTimeoutSeconds() {
        return timeoutSeconds;
    }
}
