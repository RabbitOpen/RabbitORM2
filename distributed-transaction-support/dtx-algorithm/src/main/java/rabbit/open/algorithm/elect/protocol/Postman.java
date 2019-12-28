package rabbit.open.algorithm.elect.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rabbit.open.algorithm.elect.data.ElectionPacket;
import rabbit.open.algorithm.elect.data.ElectedLeader;
import rabbit.open.algorithm.elect.data.ProtocolPacket;
import rabbit.open.algorithm.elect.data.ElectionResult;

/**
 * <b>@description 邮递员 </b>
 */
public abstract class Postman {
	
	protected Logger logger = LoggerFactory.getLogger(getClass());
			
	private Candidate candidate;
	
	/**
	 * <b>@description 发送包 </b>
	 * @param packet
	 */
	public abstract void delivery(ProtocolPacket packet);
	
	/**
	 * <b>@description 接收包  </b>
	 * @param data
	 * @return
	 */
	public void onDataRecieved(Object data) {
		if (data instanceof ElectionResult) {
			candidate.onElectionResultRecieved((ElectionResult) data);
		} else if (data instanceof ElectedLeader) {
			candidate.onElectedLeaderRecieved((ElectedLeader) data);
		} else if (data instanceof ElectionPacket) {
			candidate.onElectionPacketRecieved((ElectionPacket) data);
		}
		logger.warn("unknown packet type[{}] is recieved", data.getClass());
	}
	
	/**
	 * <b>@description 注册数据处理器 </b>
	 * @param candidate
	 */
	public void register(Candidate candidate) {
		candidate.postmanBinded(this);
		this.candidate = candidate;
	}

}
