package rabbit.open.dtx.common.nio.pub.protocol;

import rabbit.open.dtx.common.nio.client.Node;

import java.util.List;

/**
 * 集群信息
 * @author xiaoqianbin
 * @date 2019/12/27
 **/
public class ClusterMeta {

    // 集群中所有在线的服务节点
    private List<Node> nodes;

    public List<Node> getNodes() {
        return nodes;
    }

    public void setNodes(List<Node> nodes) {
        this.nodes = nodes;
    }

}
