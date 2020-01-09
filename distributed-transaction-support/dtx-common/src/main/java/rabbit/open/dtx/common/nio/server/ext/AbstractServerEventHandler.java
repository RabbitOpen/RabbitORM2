package rabbit.open.dtx.common.nio.server.ext;

import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.ProtocolData;
import rabbit.open.dtx.common.nio.pub.ext.AbstractNetEventHandler;
import rabbit.open.dtx.common.nio.pub.inter.TransactionHandler;
import rabbit.open.dtx.common.nio.server.DtxServer;
import rabbit.open.dtx.common.nio.server.handler.DataDispatcher;

/**
 * 抽象服务端时间处理器
 * @author xiaoqianbin
 * @date 2019/12/9
 **/
public abstract class AbstractServerEventHandler extends AbstractNetEventHandler {

    // 单例dispatcher
    protected DataDispatcher dispatcher = new DataDispatcher(this);

    protected DtxServer dtxServer;

    private AbstractServerTransactionHandler transactionHandler;

    // 是否返回值的开关
    private static final ThreadLocal<Boolean> ackValveContext = new ThreadLocal<>();

    // 请求id
    private static final ThreadLocal<Long> requestIdContext = new ThreadLocal<>();

    @Override
    public DataDispatcher getDispatcher() {
        return dispatcher;
    }

    @Override
    protected void executeReadTask(Runnable task) {
        task.run();
    }

    @Override
    protected void wakeUpSelector(ChannelAgent agent) {
        getDtxServer().wakeup();
    }

    /**
     * 重写数据处理流程, 支持挂起连接
     * @param	protocolData
	 * @param	agent
     * @author  xiaoqianbin
     * @date    2019/12/12
     **/
    @Override
    protected void processData(ProtocolData protocolData, ChannelAgent agent) {
        try {
            requestIdContext.set(protocolData.getRequestId());
            Object result = getDispatcher().process(protocolData);
            if (null == ackValveContext.get()) {
                agent.response(result, protocolData.getRequestId());
            }
        } catch (Exception e) {
            agent.responseError(e, protocolData.getRequestId());
        } finally {
            ackValveContext.remove();
            requestIdContext.remove();
        }
    }

    public static Long getCurrentRequestId() {
        return requestIdContext.get();
    }

    /**
     * 挂起当前请求
     * @author  xiaoqianbin
     * @date    2019/12/12
     **/
    public static void suspendRequest() {
        ackValveContext.set(true);
    }

    /**
     * 获取监听的DtxServer对象
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    protected DtxServer getDtxServer() {
        return dtxServer;
    }

    public void setDtxServer(DtxServer dtxServer) {
        this.dtxServer = dtxServer;
        if (null != transactionHandler) {
            transactionHandler.setDtxServer(dtxServer);
        }
    }

    /**
     * dtxServer 关闭后置事件
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    public abstract void onServerClosed();

    public AbstractServerTransactionHandler getTransactionHandler() {
        return transactionHandler;
    }

    public void setTransactionHandler(AbstractServerTransactionHandler transactionHandler) {
        this.transactionHandler = transactionHandler;
        dispatcher.registerInterfaceHandler(TransactionHandler.class, transactionHandler);
    }
}
