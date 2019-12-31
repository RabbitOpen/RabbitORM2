package rabbit.open.dtx.common.nio.server;

import rabbit.open.dtx.common.nio.pub.DataHandler;

import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * 服务端服务封装
 * @author xiaoqianbin
 * @date 2019/12/11
 **/
public class DtxServerWrapper {

    DtxServer server;

    private DtxServerEventHandler eventHandler;

    public DtxServerWrapper(int port, DtxServerEventHandler handler) throws IOException {
        this.eventHandler = handler;
        server = new DtxServer(port, handler);
        server.start();
    }

    @PreDestroy
    public void close() {
        server.shutdown();
    }


    /**
     * 注册数据处理器
     * @param	handler
     * @author  xiaoqianbin
     * @date    2019/12/31
     **/
    public void registerHandler(Class<?> clz, DataHandler handler) {
        this.eventHandler.getDispatcher().registerHandler(clz, handler);
    }

    public DtxServer getServer() {
        return server;
    }
}
