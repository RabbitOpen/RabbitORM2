package rabbit.open.algorithm.elect.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.algorithm.elect.data.ElectedLeader;
import rabbit.open.algorithm.elect.data.ElectionPacket;
import rabbit.open.algorithm.elect.data.ElectionResult;
import rabbit.open.algorithm.elect.data.ProtocolPacket;

/**
 * <b>@description 邮递员 </b>
 */
public abstract class Postman {
	
	protected Logger logger = LoggerFactory.getLogger(getClass());
			
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
	public abstract void sendBack(ProtocolPacket packet);

	/**
	 * <b>@description 接收包  </b>
	 * @param data
	 * @return
	 */
	public void onDataReceived(Object data) {
		if (data instanceof ElectionResult) {
			candidate.onElectionResultReceived((ElectionResult) data);
		} else if (data instanceof ElectedLeader) {
			candidate.onElectedLeaderReceived((ElectedLeader) data);
		} else if (data instanceof ElectionPacket) {
			candidate.onElectionPacketReceived((ElectionPacket) data);
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

}
