package rabbit.open.dtx.common.nio.pub.protocol;

/**
 * 获取实例id时使用的对象
 * @author xiaoqianbin
 * @date 2020/1/1
 **/
public class ClientInstance {

    private Long id;

    public ClientInstance(Long id) {
        this.id = id;
    }

    public ClientInstance() {
    }

    public Long getId() {
        return id;
    }
}
