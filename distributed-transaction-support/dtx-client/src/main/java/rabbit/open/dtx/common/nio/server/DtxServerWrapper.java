package rabbit.open.dtx.common.nio.server;

import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * 服务端服务封装
 * @author xiaoqianbin
 * @date 2019/12/11
 **/
public class DtxServerWrapper {

    DtxServer server;

    public DtxServerWrapper(int port, DtxServerEventHandler handler) throws IOException {
        server = new DtxServer(port, handler);
        server.start();
    }

    @PreDestroy
    public void close() {
        server.shutdown();
    }
}
