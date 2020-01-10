package rabbit.open.algorithm.elect.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.algorithm.elect.data.*;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <b>@description 邮递员 </b>
 */
public abstract class Postman {
	
	protected Logger logger = LoggerFactory.getLogger(getClass());

	private Map<Class<?>, Callback> dispatcher = new ConcurrentHashMap<>();

	public Postman() {
		dispatcher.put(ElectionResult.class, data -> candidate.onElectionResultReceived((ElectionResult) data));
		dispatcher.put(ElectedLeader.class, data -> candidate.onElectedLeaderReceived((ElectedLeader) data));
		dispatcher.put(ElectionPacket.class, data -> candidate.onElectionPacketReceived((ElectionPacket) data));
		dispatcher.put(HelloKitty.class, data -> candidate.onKittyReceived((HelloKitty) data));
	}
			
	private Candidate candidate;
	
	/**
	 * <b>@description 广播发送包 </b>
	 * @param packet
	 */
	public abstract void delivery(ProtocolPacket packet);

	/**
	 * 点对点response
	 * @param	packet
	 * @author  xiaoqianbin
	 * @date    2019/12/30
	 **/
	public abstract void ack(ProtocolPacket packet);

	/**
	 * <b>@description 接收包  </b>
	 * @param data
	 */
	public void onDataReceived(Object data) {
		if (null == data || null == candidate) {
			return;
		}
		if (dispatcher.containsKey(data.getClass())) {
			dispatcher.get(data.getClass()).onDataReceived(data);
		} else {
			logger.warn("unknown packet type[{}] is received", data.getClass());
		}
	}
	
	/**
	 * <b>@description 注册数据处理器 </b>
	 * @param candidate
	 */
	public void register(Candidate candidate) {
		candidate.bindPostman(this);
		this.candidate = candidate;
	}

	private interface Callback {
		void onDataReceived(Object data);
	}

}
