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

	// 预选监听信号
	private Semaphore preselectSemaphore = new Semaphore(0);

	// 运行标记
	private boolean run = true;
	
	// 选举版本号
	private final AtomicLong electionPacketVersion = new AtomicLong(0);

	// 当前已经同意的版本号
	private long agreedVersion = 0L;

	// leader的版本号
	private long leaderVersion = 0L;

	// 候选人总个数
	private int candidateSize = 1;
	
	// 同意选举包的候选人个数
	private int agreedCandidates = 0;
	
	// 收、发数据时使用的互斥锁
	private ReentrantLock electionLock = new ReentrantLock();

	// 事件监听器
	private ElectionEventListener eventListener;

	// 节点角色
	private NodeRole role = NodeRole.OBSERVER;

	// 线程名id后缀
	private static final AtomicInteger ARBITER_ID = new AtomicInteger(0);

	// keepAliveCheckingInterval
	private long keepAliveCheckingInterval = 30L;

	// LEADER 上次活跃时间
	private Long lastActiveTime = 0L;

	private String myId;

	private String leaderId;

	private KeepAliveThread keepAliveThread;

	// 预选同意的票据
	private int preselectionAgreeTickets = 0;

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
					reelectOnLeaderLost(false);
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
	 * @param 	immediately 立刻重选
	 * @author  xiaoqianbin
	 * @date    2019/12/30
	 **/
	public void reelectOnLeaderLost(boolean immediately) {
		try {
			electionLock.lock();
			if (NodeRole.FOLLOWER != role) {
				// leader节点不做心跳检测
				return;
			}
			if (immediately || System.currentTimeMillis() - lastActiveTime > keepAliveCheckingInterval * 1000) {
				logger.warn("[{}] begin to reelect leader, my role is [{}], my version is[{}]", myId, role, electionPacketVersion.get());
				setNodeRole(NodeRole.OBSERVER);
				setLeaderVersion(0L);
				setPreselectionAgreeTickets(0);
				setLeaderId(null);
				startElection();
			}
		} finally {
			electionLock.unlock();
		}
	}

	public void setPreselectionAgreeTickets(int preselectionAgreeTickets) {
		this.preselectionAgreeTickets = preselectionAgreeTickets;
	}

	public void setLeaderVersion(long leaderVersion) {
		this.leaderVersion = leaderVersion;
	}

	/**
	 * <b>@description 关闭选举 </b>
	 */
	public void shutdown() {
		logger.info("election arbiter is closing.....");
		run = false;
		electionSemaphore.release();
		sleepSemaphore.release();
		try {
			stopKeepAlive();
			join();
			logger.info("election arbiter is closed");
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
	}

	public void stopKeepAlive() throws InterruptedException {
		if (null != keepAliveThread) {
			keepAliveThread.shutdown();
			keepAliveThread = null;
		}
	}

	/**
	 * <b>@description 选举 </b>
	 * @throws InterruptedException
	 */
	private void doElection() throws InterruptedException {
		logger.debug("begin to elect");
		while (true) {
			sendElectionPacket();
			if (sleepSemaphore.tryAcquire(2000L + new Random().nextInt(2000), TimeUnit.MILLISECONDS)) {
				break;
			}
		}
		logger.debug("preselection is over, leader id: {}, elect-version: {}, my role: {}", leaderId, electionPacketVersion.get(), role);
	}

	/**
	 * <b>@description 发送选举包 </b>
	 * @return false 选举已经结束
	 */
	private void sendElectionPacket() {
		try {
			electionLock.lock();
			if (NodeRole.OBSERVER != role) {
				// 有可能在启动前就收到选举结果通知了
				return;
			}
			agreedCandidates = 1;
			postman.delivery(new ElectionPacket(electionPacketVersion.addAndGet(1L)));
			agreedVersion = electionPacketVersion.get();
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
		try {
			electionLock.lock();
			if (result.getVersion() < electionPacketVersion.get()) {
				return;
			}
			if (result.getResult() == ElectionResult.AGREE) {
				agreedCandidates++;
				if (agreedCandidates == (candidateSize / 2 + 1)) {
					// 票够了就直接唤醒投票线程, 结束投票
					this.role = NodeRole.LEADER;
					this.leaderId = myId;
					logger.info("I'm preselected to be the leader, elect version is: {}", electionPacketVersion.get());
					postman.delivery(new Preselection(electionPacketVersion.get(), myId));
					sleepSemaphore.release();
					startPreselectListener();
				}
			}
		} finally {
			electionLock.unlock();
		}
	}

	// 启动预选结果监听器
	protected void startPreselectListener() {
		Thread listener = new Thread(() -> {
			try {
				if (preselectSemaphore.tryAcquire(5, TimeUnit.SECONDS) && candidateSize - 1 != preselectionAgreeTickets) {
					logger.warn("node[{}] has been elected to be the leader already!", leaderId);
				} else {
					logger.info("nobody opposes! I'm elected to be the leader now!");
					postman.delivery(new ElectedLeader(electionPacketVersion.get(), myId));
					eventListener.onLeaderElected(this);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		});
		listener.setDaemon(true);
		listener.start();
	}

	/**
	 * <b>@description 设置keepAlive检测间隔 </b>
	 * @param keepAliveCheckingInterval
	 */
	public void setKeepAliveCheckingInterval(long keepAliveCheckingInterval) {
		this.keepAliveCheckingInterval = keepAliveCheckingInterval;
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
		try {
			electionLock.lock();
			if (!run) {
				// 正在关闭的节点拒绝一切投票
				postman.ack(new ElectionResult(ElectionResult.REJECT, electionPacket.getVersion()));
				logger.debug("[{}] received election packet({}), REJECTED!", myId, electionPacket.getVersion());
				return;
			}
			if (NodeRole.LEADER == this.role) {
				// 告诉他我就是leader
				logger.debug("Hey! I'm the king, don't revolt!");
				// 对于正常参与选举的节点可能因此重复收到ElectedLeader结果导致自身的选举信号(sleepSemaphore)重复释放
				postman.ack(new ElectedLeader(electionPacketVersion.get(), myId));
			} else {
				// 同一个版本号的数据只会被同意一次
				if (electionPacket.getVersion() <= electionPacketVersion.get() || agreedVersion >= electionPacket.getVersion()) {
					postman.ack(new ElectionResult(ElectionResult.REJECT, electionPacket.getVersion()));
					logger.debug("[{}] received election packet({}), REJECTED!", myId, electionPacket.getVersion());
				} else {
					agreedVersion = electionPacket.getVersion();
					postman.ack(new ElectionResult(ElectionResult.AGREE, electionPacket.getVersion()));
					logger.debug("[{}] received election packet({}), AGREED!", myId, electionPacket.getVersion());
				}
			}
		} finally {
			electionLock.unlock();
		}
	}

	@Override
	public void onKittyReceived(HelloKitty kitty) {
		logger.debug("hello kitty from[{}] is received!", kitty.getNodeId());
		if (kitty.getNodeId().equals(this.leaderId)) {
			updateLeaderLastActiveTime();
		}
	}

	// 更新活跃时间
	private void updateLeaderLastActiveTime() {
		lastActiveTime = System.currentTimeMillis();
	}

	/**
	 * 收到选举结果通知
	 * @param	electedLeader
	 * @author  xiaoqianbin
	 * @date    2020/1/10
	 **/
	@Override
	public void onElectedLeaderReceived(ElectedLeader electedLeader) {
		try {
			electionLock.lock();
			logger.debug("[{}] is elected to be the leader!, election version is {}", electedLeader.getNodeId(), electedLeader.getVersion());
			this.role = NodeRole.FOLLOWER;
			setLeaderId(electedLeader.getNodeId());
			// 更新活跃时间
			updateLeaderLastActiveTime();
			wakeUpOnlyOnceEachTurn();
			leaderVersion = electedLeader.getVersion();
			agreedVersion = electedLeader.getVersion();
			this.electionPacketVersion.set(electedLeader.getVersion());
		} finally {
			electionLock.unlock();
		}
	}

	/**
	 * <b>@description 收到Leader预选结果</b>
	 * @param	preselection
	 * @author  xiaoqianbin
	 * @date    2020/1/14
	 **/
	@Override
	public void onPreselectionReceived(Preselection preselection) {
		try {
			electionLock.lock();
			if (preselection.getNodeId().equals(leaderId)) {
				// 重复接收的选举通知信号直接过滤掉
				return;
			}
			logger.debug("[{}] is preselected to be the leader!, election version is {}", preselection.getNodeId(), preselection.getVersion());
			setLeaderId(preselection.getNodeId());
			// 更新活跃时间
			updateLeaderLastActiveTime();
			leaderVersion = preselection.getVersion();
			if (NodeRole.LEADER == this.role && electionPacketVersion.get() < preselection.getVersion()) {
				// 选择leader版本号小的节点为leader
				postman.ack(new PreselectionAck(myId, ElectionResult.REJECT));
			} else {
				postman.ack(new PreselectionAck(myId));
				this.role = NodeRole.FOLLOWER;
			}
			wakeUpOnlyOnceEachTurn();
		} finally {
			electionLock.unlock();
		}
	}

	// 每个周期只释放一次信号
	private void wakeUpOnlyOnceEachTurn() {
		if (0 == leaderVersion) {
			// 一次选举释放一次
			sleepSemaphore.release();
		}
	}

	/**
	 * <b>@description 预选结果确认信息</b>
	 * @param	ack
	 * @author  xiaoqianbin
	 * @date    2020/1/14
	 **/
	@Override
	public void onPreselectionAckReceived(PreselectionAck ack) {
		try {
			electionLock.lock();
			if (ElectionResult.REJECT == ack.getResult()) {
				leaderId = ack.getNodeId();
				preselectSemaphore.release();
			} else {
				preselectionAgreeTickets++;
				if (candidateSize - 1 == preselectionAgreeTickets) {
					preselectSemaphore.release();
				}
			}
		} finally {
			electionLock.unlock();
		}
	}

	public String getLeaderId() {
		return leaderId;
	}

	private void setLeaderId(String leaderId) {
		this.leaderId = leaderId;
	}
}
