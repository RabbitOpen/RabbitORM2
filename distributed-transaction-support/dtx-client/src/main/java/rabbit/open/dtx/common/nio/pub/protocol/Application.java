package rabbit.open.dtx.common.nio.pub.protocol;

/**
 * 通报的app Name
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
public class Application {

    private String name;

    public Application() {

    }

    public Application(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
