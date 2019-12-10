package rabbit.open.dtx.common.nio.client.ext;

import rabbit.open.dtx.common.nio.client.DtxClient;
import rabbit.open.dtx.common.nio.client.FutureResult;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.pub.ext.AbstractNetEventHandler;
import rabbit.open.dtx.common.nio.server.handler.DataDispatcher;

/**
 * 客户端读事件处理器
 * @author xiaoqianbin
 * @date 2019/12/8
 **/
class ClientNetEventHandler extends AbstractNetEventHandler {

    private DtxResourcePool dtxResourcePool;

    private DataDispatcher dispatcher = new DataDispatcher() {

        @Override
        public Object process(ProtocolData protocolData) {
            if (isNotifyMessage(protocolData)) {
                if (null != dtxResourcePool.getTransactionManger().getMessageListener()) {
                    dtxResourcePool.getTransactionManger().getMessageListener().onMessageReceived(protocolData.getData());
                } else {
                    logger.info("discard notify info: {}", protocolData.getData());
                }
            } else {
                FutureResult result = DtxClient.findFutureResult(protocolData.getRequestId());
                if (null != result) {
                    result.wakeUp(protocolData.getData());
                } else {
                    logger.error("unknown protocol data, {}", protocolData.getRequestId());
                }
            }
            return null;
        }

        /**
         * 是通知消息
         * @param	protocolData
         * @author  xiaoqianbin
         * @date    2019/12/10
         **/
        private boolean isNotifyMessage(ProtocolData protocolData) {
            return null == protocolData.getRequestId();
        }
    };

    public ClientNetEventHandler(DtxResourcePool dtxResourcePool) {
        this.dtxResourcePool = dtxResourcePool;
    }

    /**
     * 重写基类，不再返回信息
     * @param    protocolData
     * @param    agent
     * @author xiaoqianbin
     * @date 2019/12/8
     **/
    @Override
    protected void processData(ProtocolData protocolData, ChannelAgent agent) {
        getDispatcher().process(protocolData);
    }

    @Override
    protected DataDispatcher getDispatcher() {
        return dispatcher;
    }

    @Override
    protected void executeReadTask(Runnable task) {
        task.run();
    }

    @Override
    protected void wakeUpSelector(ChannelAgent agent) {
        agent.getSelectionKey().selector().wakeup();
    }

    @Override
    protected void closeAgentChannel(ChannelAgent agent) {
        agent.setClosed(true);
        agent.getResource().destroy();
    }

}
