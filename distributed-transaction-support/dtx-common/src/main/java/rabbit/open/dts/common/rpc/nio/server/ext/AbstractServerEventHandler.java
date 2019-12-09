package rabbit.open.dts.common.rpc.nio.server.ext;

import rabbit.open.dts.common.rpc.nio.pub.ChannelAgent;
import rabbit.open.dts.common.rpc.nio.pub.ext.AbstractNetEventHandler;
import rabbit.open.dts.common.rpc.nio.server.DtxServer;

/**
 * 抽象服务端时间处理器
 * @author xiaoqianbin
 * @date 2019/12/9
 **/
public abstract class AbstractServerEventHandler extends AbstractNetEventHandler {

    // 单例dispatcher
    protected DataDispatcher dispatcher = new DataDispatcher();

    protected DtxServer dtxServer;

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
        getDtxServer().wakeup();
    }

    @Override
    protected void closeChannel(ChannelAgent agent) {
        getDtxServer().closeChannelKey(agent.getSelectionKey());
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
    }

    /**
     * dtxServer 关闭后置事件
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    public abstract void onServerClosed();
}
