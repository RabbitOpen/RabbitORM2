package rabbit.open.dtx.client.test.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NettyClient {
	
	static Logger logger = LoggerFactory.getLogger(NettyClient.class);
	
	public static void main(String[] args) {
        startClient();
    }

    public static void startClient(){
        //1.定义服务类
        Bootstrap clientBootstap = new Bootstrap();

        //2.定义执行线程组
        EventLoopGroup worker = new NioEventLoopGroup();

        //3.设置线程池
        clientBootstap.group(worker);

        //4.设置通道
        clientBootstap.channel(NioSocketChannel.class);

        //5.添加Handler
        clientBootstap.handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
            	logger.info("client channel init!");
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new MyByteToMessageEncoder());
				pipeline.addLast(new MyByteToMessageDecoder());
                pipeline.addLast(new ClientHandler());
            }
        });

        //6.建立连接
        clientBootstap.connect("localhost",9099);
        
    }

    static class ClientHandler extends ChannelInboundHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        	logger.info("client receive msg:" + msg.toString());
        }

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			Car c = new Car();c.setNo(NettyClient.class.getSimpleName());
			ctx.writeAndFlush(c);
		}
    }
}
