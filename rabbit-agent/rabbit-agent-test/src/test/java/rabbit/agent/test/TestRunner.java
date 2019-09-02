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
        try {
            s.tryAcquire(1, 10, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void h3() {
        h4();
        h4();
        try {
        	s.tryAcquire(1, 10, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void h4() {
        try {
        	s.tryAcquire(1, 10, TimeUnit.MICROSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
