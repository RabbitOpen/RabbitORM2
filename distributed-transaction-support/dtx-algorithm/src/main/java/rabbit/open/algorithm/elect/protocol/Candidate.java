package rabbit.open.algorithm.elect.protocol;

import rabbit.open.algorithm.elect.data.*;

/**
 * <b>@description 候选者  </b>
 */
interface Candidate {
	
	/**
	 * <b>@description 接收ElectionResult数据 </b>
	 * @param result
	 */
	void onElectionResultReceived(ElectionResult result);

	/**
	 * <b>@description 接收到选举票 </b>
	 * @param electionPacket
	 */
	void onElectionPacketReceived(ElectionPacket electionPacket);
	
	/**
	 * <b>@description 收到Leader发送的leader结果通知 </b>
	 * @param electedLeader
	 */
	void onElectedLeaderReceived(ElectedLeader electedLeader);

	/**
	 * <b>@description 收到Leader预选结果</b>
	 * @param preselection
	 */
	void onPreselectionReceived(Preselection preselection);

	/**
	 * <b>@description 预选结果确认信息</b>
	 * @param ack
	 */
	void onPreselectionAckReceived(PreselectionAck ack);

	/**
	 * 接收到心跳包
	 * @param	kitty
	 * @author  xiaoqianbin
	 * @date    2019/12/30
	 **/
	void onKittyReceived(HelloKitty kitty);

	/**
	 * <b>@description 接收器和处理器的绑定桥接方法 </b>
	 * @param postman
	 */
	void bindPostman(Postman postman);
}
