package rabbit.open.dts.common.rpc.nio.client;

import java.util.concurrent.Semaphore;

/**
 * 通信对象
 * @author xiaoqianbin
 * @date 2019/12/8
 **/
public class FutureResult {

    private Semaphore semaphore = new Semaphore(0);

    private Object data;

    public Object getData() throws InterruptedException {
        semaphore.acquire();
        return data;
    }

    public void wakeUp(Object data) {
        this.data = data;
        semaphore.release();
    }
}
