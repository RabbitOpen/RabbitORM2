package rabbit.open.dtx.common.nio.pub.protocol;

/**
 * 通报的app Name
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
public class Application {

    private String name;

    private Long instanceId;

    public Application() {

    }

    public Application(String name, Long instanceId) {
        this.name = name;
        this.instanceId = instanceId;
    }

    public String getName() {
        return name;
    }

    public Long getInstanceId() {
        return instanceId;
    }
}
