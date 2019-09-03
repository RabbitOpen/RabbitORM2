package rabbit.agent.test;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class TestRunner {

	private Semaphore s = new Semaphore(0);

    public void h1() {
        h2();
        h4();
        h2();
    }
    public void h2() {
        h3();
        sleep(10);
    }

    private void sleep(long mils) {
        try {
            s.tryAcquire(1, mils, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void h3() {
        h4();
        h4();
        sleep(10);
    }
    public void h4() {
        sleep(10);
    }
}
