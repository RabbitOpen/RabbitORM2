package rabbit.open.dtx.common.nio.pub.protocol;

/**
 * 通报的app Name
 * @author xiaoqianbin
 * @date 2019/12/10
 **/
public class Application {

    private String name;

    private Long instanceId;

    private String hostName;

    private int port;

    public Application() {

    }

    public Application(String name, Long instanceId) {
        this(name, instanceId, null, 0);
    }

    public Application(String name, Long instanceId, String hostName, int port) {
        this.name = name;
        this.instanceId = instanceId;
        this.hostName = hostName;
        this.port = port;
    }

    public String getName() {
        return name;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public String getHostName() {
        return hostName;
    }

    public int getPort() {
        return port;
    }
}
