package rabbit.open.algorithm.elect.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.algorithm.elect.data.*;
import rabbit.open.algorithm.elect.event.ElectionEventListener;

import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * <b>@description 选举仲裁者  </b>
 */
public class ElectionArbiter extends Thread implements Candidate {
	
	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	protected Postman postman;

	// sleep信号
	private Semaphore sleepSemaphore = new Semaphore(0);
	
	// 选举信号
	private Semaphore electionSemaphore = new Semaphore(0);
	
	private boolean run = true;
	
	// 选举版本号
	private final AtomicLong electionPacketVersion = new AtomicLong(0);

	// 当前已经同意的版本号
	private long agreedVersion = 0L;
	
	// 候选人总个数
	private int candidateSize = 1;
	
	// 同意选举包的候选人个数
	private int agreedCandidates = 0;
	
	// 选举的锁
	private ReentrantLock electionLock = new ReentrantLock();

	// 事件监听器
	private ElectionEventListener eventListener;
	
	// 节点角色
	private NodeRole role = NodeRole.OBSERVER;

	// 线程名id后缀
	private static final AtomicInteger ARBITER_ID = new AtomicInteger(0);

	// keepAliveCheckingInterval
	private long keepAliveCheckingInterval = 10L;

	// LEADER 上次活跃时间
	private Long lastActiveTime = 0L;

	private String myId;

	private String leaderId;

	private KeepAliveThread keepAliveThread;

	public ElectionArbiter(int candidateSize, String nodeId, LeaderElectedListener listener) {
		super(ElectionArbiter.class.getSimpleName() + "-" + ARBITER_ID.getAndIncrement());
		this.eventListener = listener;
		this.candidateSize = candidateSize;
		myId = nodeId;
		start();
	}

	/**
	 * 返回自己的id
	 * @author  xiaoqianbin
	 * @date    2019/12/30
	 **/
	public String getNodeId() {
		return myId;
	}

	/**
	 * <b>@description 开始选举 </b>
	 */
	public void startElection() {
		electionSemaphore.release();
	}

	public NodeRole getNodeRole() {
		return role;
	}

	public void setNodeRole(NodeRole role) {
		this.role = role;
	}

	@Override
	public void run() {
		while (true) {
			try {
				if (electionSemaphore.tryAcquire(keepAliveCheckingInterval, TimeUnit.SECONDS)) {
					if (run) {
						doElection();
					} else {
						return;
					}
				} else {
					// leader丢失以后就重新选举
					reelectOnLeaderLost();
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * 启动心跳线程
	 * @author  xiaoqianbin
	 * @date    2019/12/30
	 **/
	protected void startKeepAliveThread() {
		if (null == keepAliveThread && NodeRole.LEADER == role) {
			keepAliveThread = new KeepAliveThread(this);
			keepAliveThread.setDaemon(true);
			keepAliveThread.start();
		}
	}

	/**
	 * leader丢失以后就重新选举
	 * @author  xiaoqianbin
	 * @date    2019/12/30
	 **/
	private void reelectOnLeaderLost() {
		if (NodeRole.FOLLOWER != role) {
			// leader节点不做心跳检测
			return;
		}
		if (System.currentTimeMillis() - lastActiveTime > keepAliveCheckingInterval * 1000) {
			logger.warn("[{}] begin to reelect leader, my role is [{}]", myId, role);
			setNodeRole(NodeRole.OBSERVER);
			setLeaderId(null);
			startElection();
		}
	}

	/**
	 * keep alive checking interval (second)
	 * @param	keepAliveCheckingInterval
	 * @author  xiaoqianbin
	 * @date    2019/12/30
	 **/
	public void setKeepAliveCheckingInterval(long keepAliveCheckingInterval) {
		this.keepAliveCheckingInterval = keepAliveCheckingInterval;
	}

	/**
	 * <b>@description 关闭选举 </b>
	 */
	public void shutdown() {
		logger.info("election arbiter is closing.....");
		run = false;
		electionSemaphore.release();
		try {
			if (null != keepAliveThread) {
				keepAliveThread.shutdown();
			}
			join();
			logger.info("election arbiter is closed");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}
	
	/**
	 * <b>@description 选举 </b>
	 * @throws InterruptedException
	 */
	private void doElection() throws InterruptedException {
		logger.debug("begin to elect");
		eventListener.onElectionBegin();
		while (true) {
			sendElectionPacket();
			if (randomHoldOn()) {
				break;
			}
		}
		eventListener.onElectionEnd(this);
		logger.debug("election is over, leader id: {}, elect-version: {}", leaderId, electionPacketVersion.get());
	}

	/**
	 * <b>@description 随机等待2-4秒 </b>
	 * @throws InterruptedException
	 */
	private boolean randomHoldOn() throws InterruptedException {
		return sleepSemaphore.tryAcquire(2000L + new Random().nextInt(2000), TimeUnit.MILLISECONDS);
	}

	/**
	 * <b>@description 发送选举包 </b>
	 * @return false 选举已经结束
	 */
	private void sendElectionPacket() {
		try {
			electionLock.lock();
			agreedCandidates = 1;
			if (NodeRole.OBSERVER != role) {
				// 有可能在启动前就收到选举结果通知了
				return;
			}
			postman.delivery(new ElectionPacket(electionPacketVersion.addAndGet(1L)));
			logger.debug("send ElectionPacket({})", electionPacketVersion.get());
		} finally {
			electionLock.unlock();
		}
	}

	/**
	 * <b>@description 接收到投票结果 </b>
	 */
	@Override
	public void onElectionResultReceived(ElectionResult result) {
		if (result.getVersion() < electionPacketVersion.get()) {
			return;
		}
		electionLock.lock();
		if (result.getResult() == ElectionResult.AGREE) {
			agreedCandidates++;
			if (agreedCandidates == (candidateSize / 2 + 1)) {
				// 票够了就直接唤醒投票线程, 结束投票
				this.role = NodeRole.LEADER;
				this.leaderId = myId;
				logger.info("I'm elected to be the leader, elect version is: {}", electionPacketVersion.get());
				postman.delivery(new ElectedLeader(electionPacketVersion.get(), myId));
				sleepSemaphore.release();
			}
		}
		electionLock.unlock();
	}

	@Override
	public void bindPostman(Postman postman) {
		this.postman = postman;
	}

	/**
	 * <b>@description 接收到选举票 </b>
	 * @param electionPacket
	 */
	@Override
	public void onElectionPacketReceived(ElectionPacket electionPacket) {
		if (NodeRole.LEADER == this.role) {
			// 告诉他我就是leader
			postman.ack(new ElectedLeader(electionPacketVersion.get(), myId));
		} else {
			electionLock.lock();
			// 同一个版本号的数据只会被同意一次
			if (electionPacket.getVersion() <= electionPacketVersion.get() || agreedVersion >= electionPacket.getVersion()) {
				postman.ack(new ElectionResult(ElectionResult.REJECT, electionPacket.getVersion()));
				logger.debug("received election packet({}), REJECTED!", electionPacket.getVersion());
			} else {
				agreedVersion = electionPacket.getVersion();
				postman.ack(new ElectionResult(ElectionResult.AGREE, electionPacket.getVersion()));
				logger.debug("received election packet({}), AGREED!", electionPacket.getVersion());
			}
			electionLock.unlock();
		}
	}

	@Override
	public void onKittyReceived(HelloKitty kitty) {
		logger.debug("helloKitty from [{}] received!", kitty.getNodeId());
		if (kitty.getNodeId().equals(this.leaderId)) {
			lastActiveTime = System.currentTimeMillis();
		}
	}

	@Override
	public void onElectedLeaderReceived(ElectedLeader electedLeader) {
		electionLock.lock();
		this.role = NodeRole.FOLLOWER;
		setLeaderId(electedLeader.getNodeId());
		lastActiveTime = System.currentTimeMillis();
		this.electionPacketVersion.set(electedLeader.getVersion());
		sleepSemaphore.release();
		electionLock.unlock();
	}

	private void setLeaderId(String leaderId) {
		this.leaderId = leaderId;
	}
}
