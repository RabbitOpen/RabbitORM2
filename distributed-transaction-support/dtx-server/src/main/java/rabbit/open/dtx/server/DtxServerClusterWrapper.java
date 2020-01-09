package rabbit.open.dtx.server;

import rabbit.open.algorithm.elect.data.NodeRole;
import rabbit.open.algorithm.elect.protocol.ElectionArbiter;
import rabbit.open.algorithm.elect.protocol.LeaderElectedListener;
import rabbit.open.dtx.common.nio.client.AbstractMessageListener;
import rabbit.open.dtx.common.nio.client.MessageListener;
import rabbit.open.dtx.common.nio.client.Node;
import rabbit.open.dtx.common.nio.client.ext.AbstractTransactionManager;
import rabbit.open.dtx.common.nio.client.ext.ClientNetEventHandler;
import rabbit.open.dtx.common.nio.client.ext.DtxChannelAgentPool;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.DataHandler;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.pub.protocol.Coordination;
import rabbit.open.dtx.common.nio.server.DtxServerEventHandler;
import rabbit.open.dtx.common.nio.server.handler.DataDispatcher;
import rabbit.open.dtx.server.handler.RedisTransactionHandler;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * 分布式server包装器
 * @author xiaoqianbin
 * @date 2020/1/6
 **/
public class DtxServerClusterWrapper extends DtxServerWrapper {

//    private Logger logger = LoggerFactory.getLogger(getClass());

    private DtxChannelAgentPool channelAgentPool;

    public static final String CLUSTER_SERVER_APP_NAME = "DtxServerCluster";

    private ElectionArbiter arbiter;

    private List<Node> nodes;

    private RabbitPostman postman;

    private ArrayBlockingQueue<Node> nodeList = new ArrayBlockingQueue<>(128);

    private int port;

    public DtxServerClusterWrapper(int port, DtxServerEventHandler handler, int candidateSize, List<Node> nodes) throws IOException {
        this(InetAddress.getLocalHost().getHostAddress(), port, handler, candidateSize, nodes);
    }

    public DtxServerClusterWrapper(String hostName, int port, DtxServerEventHandler handler, int candidateSize, List<Node> nodes) throws IOException {
        super(hostName, port, handler);
        this.nodes = nodes;
        nodeList.addAll(nodes);
        this.port = port;
        this.postman = new RabbitPostman(this);
        initClientPool();
        arbiter = new ElectionArbiter(candidateSize, getServer().getServerId(), new LeaderElectedListener() {

            @Override
            public void onElectionEnd(ElectionArbiter arbiter1) {
                super.onElectionEnd(arbiter1);
                if (NodeRole.LEADER == arbiter1.getNodeRole()) {
                    RedisTransactionHandler transactionHandler = (RedisTransactionHandler) handler.getTransactionHandler();
                    transactionHandler.startSweeper();
                }
            }

        });
        postman.register(arbiter);
        arbiter.startElection();
    }

    @Override
    protected void beforeServerStart() {

        // 注册一个选举包处理器
        registerServerDataHandler(Coordination.class, data -> {
            postman.onDataReceived(((Coordination)data.getData()).getProtocolPacket());
            return new Coordination(postman.getResponse());
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
    @SuppressWarnings({ "rawtypes", "serial" })
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
        }, eventHandler, listener);
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
