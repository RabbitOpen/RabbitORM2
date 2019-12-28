package rabbit.open.algorithm.elect.protocol;

import rabbit.open.algorithm.elect.data.ElectionPacket;
import rabbit.open.algorithm.elect.data.ElectedLeader;
import rabbit.open.algorithm.elect.data.ElectionResult;

/**
 * <b>@description 候选者  </b>
 * @param <T>
 */
interface Candidate {
	
	/**
	 * <b>@description 接收ElectionResult数据 </b>
	 * @param result
	 */
	void onElectionResultRecieved(ElectionResult result);

	/**
	 * <b>@description 接收到选举票 </b>
	 * @param electionPacket
	 */
	void onElectionPacketRecieved(ElectionPacket electionPacket);
	
	/**
	 * <b>@description 收到Leader发送的leader结果通知 </b>
	 * @param result
	 */
	void onElectedLeaderRecieved(ElectedLeader result);
	
	/**
	 * <b>@description 接收器和处理器的绑定桥接方法 </b>
	 * @param postman
	 */
	void postmanBinded(Postman postman);
}
