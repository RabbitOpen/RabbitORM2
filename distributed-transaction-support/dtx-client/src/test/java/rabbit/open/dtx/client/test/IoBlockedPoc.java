package rabbit.open.dtx.client.test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rabbit.open.dtx.common.nio.client.FutureResult;
import rabbit.open.dtx.common.nio.client.ext.DtxChannelAgentPool;
import rabbit.open.dtx.common.nio.pub.ChannelAgent;
import rabbit.open.dtx.common.nio.pub.NioSelector;
import rabbit.open.dtx.common.nio.pub.protocol.KeepAlive;
import rabbit.open.dtx.common.nio.server.DtxServer;
import rabbit.open.dtx.common.nio.server.DtxServerEventHandler;

/**
 * 
 * <b>@description 模拟有一个io堵塞了的场景，检测服务端是否为被挂起 </b>
 */
public class IoBlockedPoc {

	static NioSelector nioSelector;
	static MyClientNetEventHandler handler = new MyClientNetEventHandler();
	static int port = 10003;
	static Semaphore semaphore = new Semaphore(0);
	private static Logger logger = LoggerFactory.getLogger(IoBlockedPoc.class);

	public static void main(String[] args) throws IOException, InterruptedException { 
		startServer();
		startNioClientSelector();
		sendBadPacket();

		MyTestTransactionManager manager = new MyTestTransactionManager();
		DtxChannelAgentPool cap = new DtxChannelAgentPool(manager);
		for (int i = 0; i < 10000; i++) {
			try {
				ChannelAgent agent = cap.getResource();
				FutureResult result = agent.send(new KeepAlive());
				agent.release();
				result.getData();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		logger.info("read over");

	}

	// 发送脏数据（只发一部分）
	protected static void sendBadPacket() throws IOException, ClosedChannelException, InterruptedException {
		SocketChannel channel = SocketChannel.open();
		// 设置通道为非阻塞
		channel.configureBlocking(false);
		channel.register(nioSelector.getRealSelector(), SelectionKey.OP_CONNECT);
		channel.connect(new InetSocketAddress("localhost", port));
		semaphore.acquire();
		ByteBuffer buffer = ByteBuffer.allocate(4);
		buffer.putInt(100);
		buffer.position(0);
		channel.write(buffer);
		logger.info("write half data");
	}

	protected static void startServer() throws IOException {
		DtxServerEventHandler handler = new DtxServerEventHandler();
		handler.init();
		handler.setBossCoreSize(1);
		handler.setMaxBossConcurrence(1);
		DtxServer server = new DtxServer(port, handler);
		server.start();
	}

	protected static void startNioClientSelector() throws IOException {
		nioSelector = new NioSelector(Selector.open());
		Thread readThread = new Thread(() -> {
			while (true) {
				try {
					read();
				} catch (Exception e) {
				}
			}
		});
		readThread.start();
	}

	private static void read() throws IOException {
		nioSelector.select(1000);
		Iterator<?> iterator = nioSelector.selectedKeys().iterator();
		while (iterator.hasNext()) {
			SelectionKey key = (SelectionKey) iterator.next();
			iterator.remove();
			if (!key.isValid()) {
				continue;
			}
			SocketChannel channel = (SocketChannel) key.channel();
			if (channel.finishConnect()) {
				channel.register(nioSelector.getRealSelector(), SelectionKey.OP_READ);
				semaphore.release();
			}
			if (key.isReadable()) {
				SocketChannel sc = (SocketChannel) key.channel();
				int len = sc.read(ByteBuffer.allocate(100));
				if (-1 == len) {
					sc.close();
					key.cancel();
					logger.info("坏连接已经关闭");
				}
			}
		}
	}

}
