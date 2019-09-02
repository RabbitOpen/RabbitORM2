package rabbit.agent.test;

public class TestRunner {


    public void h1() {
        h2();
        h4();
        h2();
    }
    public void h2() {
        h3();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void h3() {
        h4();
        h4();
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
    public void h4() {
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
