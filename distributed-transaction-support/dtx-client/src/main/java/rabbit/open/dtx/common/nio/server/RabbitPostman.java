package rabbit.open.dtx.common.nio.server;

import rabbit.open.algorithm.elect.data.ProtocolPacket;
import rabbit.open.algorithm.elect.protocol.Postman;
import rabbit.open.dtx.common.nio.client.Node;
import rabbit.open.dtx.common.nio.pub.protocol.Coordination;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Set;

/**
 * 数据发送对象(基于DtxChannelAgentPool的Postman)
 * @author xiaoqianbin
 * @date 2020/1/7
 **/
public class RabbitPostman extends Postman {

    private ClusterDtxServerWrapper dtxServerWrapper;

    private ThreadLocal<ProtocolPacket> dataContext = new ThreadLocal<>();

    private Set<String> hostAddresses = new HashSet<>();

    public RabbitPostman(ClusterDtxServerWrapper dtxServerWrapper) throws UnknownHostException {
        this.dtxServerWrapper = dtxServerWrapper;
        // 127.0.0.1
        hostAddresses.add(InetAddress.getLoopbackAddress().getHostAddress());
        // ipv4
        hostAddresses.add(InetAddress.getLocalHost().getHostAddress());
        hostAddresses.add("localhost");
        for (InetAddress address : InetAddress.getAllByName("localhost")) {
            hostAddresses.add(address.getHostName());
        }
    }

    @Override
    public void delivery(ProtocolPacket packet) {
        for (Node node : dtxServerWrapper.getChannelAgentPool().getNodes()) {
            if (isLocalhost(node) || node.isIsolated()) {
                continue;
            }
            try {
                node.getBoundAgent().send(new Coordination(packet));
            } catch (Exception e) {
                node.getBoundAgent().destroy();
            }
        }
    }

    private boolean isLocalhost(Node node) {
        return hostAddresses.contains(node.getHost()) && node.getPort() == dtxServerWrapper.getPort();
    }

    @Override
    public void ack(ProtocolPacket packet) {
        dataContext.remove();
        dataContext.set(packet);
    }

    public ProtocolPacket getResponse() {
        ProtocolPacket packet = dataContext.get();
        dataContext.remove();
        return packet;
    }
}
