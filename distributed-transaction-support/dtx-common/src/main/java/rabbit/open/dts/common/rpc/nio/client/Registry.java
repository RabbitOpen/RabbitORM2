package rabbit.open.dts.common.rpc.nio.client;

import java.util.List;

/**
 * rpc 注册信息
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class Registry {

    // 服务器节点
    private List<Node> nodes;

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }
}
