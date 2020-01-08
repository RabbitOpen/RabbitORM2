package rabbit.open.dtx.common.nio.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.algorithm.elect.data.NodeRole;
import rabbit.open.algorithm.elect.protocol.ElectionArbiter;
import rabbit.open.algorithm.elect.protocol.LeaderElectedListener;
import rabbit.open.dtx.common.nio.client.AbstractMessageListener;
import rabbit.open.dtx.common.nio.client.MessageListener;
import rabbit.open.dtx.common.nio.client.Node;
import rabbit.open.dtx.common.nio.client.ext.AbstractTransactionManager;
import rabbit.open.dtx.common.nio.client.ext.ClientNetEventHandler;
import rabbit.open.dtx.common.nio.client.ext.DtxChannelAgentPool;
import rabbit.open.dtx.common.nio.pub.CallHelper;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.DataHandler;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.pub.protocol.ClusterMeta;
import rabbit.open.dtx.common.nio.pub.protocol.Coordination;
import rabbit.open.dtx.common.nio.server.ext.AbstractServerEventHandler;
import rabbit.open.dtx.common.nio.server.handler.DataDispatcher;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 分布式server包装器
 * @author xiaoqianbin
 * @date 2020/1/6
 **/
public class ClusterDtxServerWrapper extends DtxServerWrapper {

    private Logger logger = LoggerFactory.getLogger(getClass());

    private DtxChannelAgentPool channelAgentPool;

    public static final String CLUSTER_SERVER_APP_NAME = "ClusterDtxServer";

    private ElectionArbiter arbiter;

    private List<Node> nodes;

    private RabbitPostman postman;

    private ArrayBlockingQueue<Node> nodeList = new ArrayBlockingQueue<>(128);

    private String hostName;

    private int port;

    public ClusterDtxServerWrapper(int port, DtxServerEventHandler handler, int candidateSize, List<Node> nodes) throws IOException {
        this(InetAddress.getLocalHost().getHostAddress(), port, handler, candidateSize, nodes);
    }

    public ClusterDtxServerWrapper(String hostName, int port, DtxServerEventHandler handler, int candidateSize, List<Node> nodes) throws IOException {
        super(hostName, port, handler);
        this.nodes = nodes;
        this.port = port;
        this.hostName = hostName;
        this.postman = new RabbitPostman(this);
        initClientPool();
        arbiter = new ElectionArbiter(candidateSize, getServer().getServerId(), new LeaderElectedListener() {

            @Override
            public void onCandidatesChanged() {
                for (ChannelAgent agent : getServer().getServerAgentMonitor().getAgents()) {
                    ClusterMeta meta = new ClusterMeta();
                    meta.setNodes(new ArrayList<>(nodeList));
                    CallHelper.ignoreExceptionCall(() -> agent.notify(meta));
                }
                logger.info("candidates changed! current node info is {}", nodeList);
            }

            @Override
            public void onElectionEnd(ElectionArbiter arbiter) {
                super.onElectionEnd(arbiter);
                if (NodeRole.LEADER == arbiter.getNodeRole()) {
                    RedisTransactionHandler transactionHandler = (RedisTransactionHandler) handler.getTransactionHandler();
                    transactionHandler.startSweeper();
                    onCandidatesChanged();
                }
            }

        });
        arbiter.bindPostman(postman);
    }

    @Override
    protected void beforeServerStart() {
        // 注册一个选举包处理器
        registerServerDataHandler(Coordination.class, data -> {
            postman.onDataReceived(data.getData());
            return postman.getResponse();
        });

        // 节点通报信息处理
        registerServerDataHandler(Node.class, data -> {
            Node nodeInfo = (Node) data.getData();
            AbstractServerEventHandler.getCurrentAgent().addShutdownHook(() -> nodeList.remove(nodeInfo));
            for (Node info : nodeList) {
                if (info.getHost().equals(nodeInfo.getHost()) && info.getPort() == nodeInfo.getPort() && nodeList.remove(info)) {
                    // 服务器节点异常重启，还未来得及清理。
                    logger.warn("remove invalid node info[{}:{}]", nodeInfo.getHost(), nodeInfo.getPort());
                }
                nodeList.add(nodeInfo);
                logger.info("node[{}:{}] joined cluster", nodeInfo.getHost(), nodeInfo.getPort());
            }
            return null;
        });
    }

    private void registerServerDataHandler(Class<?> clz, DataHandler handler) {
        getServer().getNetEventHandler().getDispatcher().registerHandler(clz, handler);
    }

    @PreDestroy
    @Override
    public void close() {
        arbiter.shutdown();
        channelAgentPool.gracefullyShutdown();
        super.close();
        RedisTransactionHandler transactionHandler = (RedisTransactionHandler) eventHandler.getTransactionHandler();
        transactionHandler.destroy();
    }

    /**
     * 初始化集群节点通信时使用的客户端连接池
     * @author xiaoqianbin
     * @date 2020/1/6
     **/
    private void initClientPool() throws IOException {

        ClusterClientNetEventHandler eventHandler = new ClusterClientNetEventHandler();

        // 注册客户端协调数据处理逻辑
        Map<Class<?>, MessageListener> listener = new HashMap<>();
        listener.put(Coordination.class, msg -> postman.onDataReceived(((Coordination)msg).getProtocolPacket()));

        channelAgentPool = new DtxChannelAgentPool(new AbstractTransactionManager() {

            @Override
            protected long getRpcTimeoutSeconds() {
                return 10L;
            }

            @Override
            public AbstractMessageListener getMessageListener() {
                return null;
            }

            @Override
            public List<Node> getServerNodes() {
                return nodes;
            }

            @Override
            public String getApplicationName() {
                return CLUSTER_SERVER_APP_NAME;
            }
        }, eventHandler, listener) {

            // 重写通报app信息逻辑
            @Override
            protected void reportAppInfo(ChannelAgent agent) throws InterruptedException {
                super.reportAppInfo(agent);
                // 通报当前节点信息
                agent.send(new Node(hostName, port)).getData(3L);
            }
        };
    }

    private class ClusterClientNetEventHandler extends ClientNetEventHandler {

        public ClusterClientNetEventHandler() {
            initDispatcher();
        }

        /**
         * 重写notify message的判定方式
         * @param	protocolData
         * @author  xiaoqianbin
         * @date    2020/1/7
         **/
        @Override
        protected boolean isNotifyMessage(ProtocolData protocolData) {
            boolean result = super.isNotifyMessage(protocolData) || protocolData.getData() instanceof Coordination;
            if (protocolData.getData() instanceof Coordination) {
                // 清除等待队列
                ChannelAgent.findFutureResult(protocolData.getRequestId());
            }
            return result;
        }

        @Override
        public DataDispatcher getDispatcher() {
            return super.getDispatcher();
        }
    }

    public DtxChannelAgentPool getChannelAgentPool() {
        return channelAgentPool;
    }

    public int getPort() {
        return port;
    }
}
