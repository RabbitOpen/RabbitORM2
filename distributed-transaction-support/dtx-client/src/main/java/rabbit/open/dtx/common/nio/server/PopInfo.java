package rabbit.open.dtx.common.nio.server;

/**
 * Pop的结果
 * @author xiaoqianbin
 * @date 2020/1/2
 **/
public class PopInfo {

    String result;

    // result的next
    String next;

    public PopInfo(String result, String next) {
        this.result = result;
        this.next = next;
    }

    public String getResult() {
        return result;
    }

    public String getNext() {
        return next;
    }
}
