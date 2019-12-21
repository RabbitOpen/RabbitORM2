package rabbit.open.dtx.client.test.netty;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;

import com.alibaba.dubbo.rpc.RpcException;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.io.Output;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

public class MyByteToMessageEncoder extends MessageToByteEncoder<Car> {

	@Override
	protected void encode(ChannelHandlerContext ctx, Car msg, ByteBuf out) throws Exception {
		byte[] body = convertToBytes(msg); // 将对象转换为byte
		int dataLength = body.length; // 读取消息的长度
		out.writeInt(dataLength); // 先将消息长度写入，也就是消息头
		out.writeBytes(body); // 消息体中包含我们要发送的数据
	}

	private byte[] convertToBytes(Car car) {
		ByteArrayOutputStream bos = null;
		Output output = null;
		try {
			bos = new ByteArrayOutputStream();
			output = new Output(bos);
			new Kryo().writeObject(output, car);
			output.flush();

			return bos.toByteArray();
		} catch (KryoException e) {
			throw new RpcException();
		} finally {
			closeQuietly(output);
			closeQuietly(bos);
		}
	}

	public void closeQuietly(Closeable c) {
		try {
			if (null != c) {
				c.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
