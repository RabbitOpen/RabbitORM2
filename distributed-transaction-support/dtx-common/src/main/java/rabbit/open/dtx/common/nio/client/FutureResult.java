package rabbit.open.dtx.common.nio.client;

import rabbit.open.dtx.common.nio.exception.TimeoutException;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * 通信对象
 * @author xiaoqianbin
 * @date 2019/12/8
 **/
public class FutureResult {

    private Semaphore semaphore = new Semaphore(0);

    private Object data;

    private Runnable timeoutCallBack;

    public FutureResult(Runnable timeoutCallBack) {
        this.timeoutCallBack = timeoutCallBack;
    }

    public Object getData() throws InterruptedException {
        return getData(0L);
    }

    public Object getData(long timeoutSeconds) throws InterruptedException {
        if (0 == timeoutSeconds) {
            semaphore.acquire();
            return data;
        }
        if (semaphore.tryAcquire(timeoutSeconds, TimeUnit.SECONDS)) {
            return data;
        } else {
            timeoutCallBack.run();
            throw new TimeoutException(timeoutSeconds);
        }
    }

    public void wakeUp(Object data) {
        this.data = data;
        semaphore.release();
    }
}
