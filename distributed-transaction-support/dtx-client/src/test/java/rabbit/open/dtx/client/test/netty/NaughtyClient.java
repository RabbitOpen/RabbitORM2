package rabbit.open.dtx.client.test.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * 
 * <b>@description 发送错误数据 </b>
 */
public class NaughtyClient {
	
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
                System.out.println("client channel init!");
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new ErrorEncoder());
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
            System.out.println("client receive msg:" + msg.toString());
        }

		@Override
		public void channelActive(ChannelHandlerContext ctx) throws Exception {
			Car c = new Car();c.setNo("abc");
			ctx.writeAndFlush(c);
		}
    }
}
