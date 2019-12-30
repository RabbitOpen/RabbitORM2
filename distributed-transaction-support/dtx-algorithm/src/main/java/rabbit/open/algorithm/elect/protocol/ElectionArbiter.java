package rabbit.open.algorithm.elect.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.algorithm.elect.data.ElectedLeader;
import rabbit.open.algorithm.elect.data.ElectionPacket;
import rabbit.open.algorithm.elect.data.ElectionResult;
import rabbit.open.algorithm.elect.data.NodeRole;
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
	
	private Postman postman;
	
	private Semaphore sleepSemaphore = new Semaphore(0);
	
	// 选举信号
	private Semaphore electionSemaphore = new Semaphore(0);
	
	private boolean run = true;
	
	// 选举版本号
	private final AtomicLong electionPacketVersion = new AtomicLong(0);
	
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

	private static final AtomicInteger ARBITER_ID = new AtomicInteger(0);

	public ElectionArbiter(int candidateSize, ElectionEventListener listener) {
		super(ElectionArbiter.class.getSimpleName() + "-" + ARBITER_ID.getAndIncrement());
		this.eventListener = listener;
		this.candidateSize = candidateSize;
		start();
	}
	
	/**
	 * <b>@description 开始选举 </b>
	 */
	public void startElection() {
		electionSemaphore.release();
	}
	
	@Override
	public void run() {
		while (true) {
			try {
				if (electionSemaphore.tryAcquire(30, TimeUnit.SECONDS)) {
					if (run) {
						role = NodeRole.OBSERVER;
						doElection();
					} else {
						return;
					}
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

	/**
	 * <b>@description 关闭选举 </b>
	 */
	public void shutdown() {
		logger.info("election arbiter is closing.....");
		run = false;
		electionSemaphore.release();
		try {
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
		logger.debug("election is over");
	}

	/**
	 * <b>@description 随机等待2-4秒 </b>
	 * @return
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
			// 清空信号量
			sleepSemaphore.drainPermits();
			if (NodeRole.OBSERVER != role) {
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
				postman.delivery(new ElectedLeader(electionPacketVersion.get()));
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
			postman.sendBack(new ElectedLeader(electionPacketVersion.get()));
		} else {
			if (electionPacket.getVersion() <= electionPacketVersion.get()) {
				postman.sendBack(new ElectionResult(ElectionResult.REJECT, electionPacket.getVersion()));
				logger.debug("received election packet({}), REJECTED!", electionPacket.getVersion());
			} else {
				postman.sendBack(new ElectionResult(ElectionResult.AGREE, electionPacket.getVersion()));
				logger.debug("received election packet({}), AGREED!", electionPacket.getVersion());
			}
		}
	}

	public NodeRole getNodeRole() {
		return role;
	}

	@Override
	public void onElectedLeaderReceived(ElectedLeader result) {
		electionLock.lock();
		this.role = NodeRole.FOLLOWER;
		this.electionPacketVersion.set(result.getVersion());
		sleepSemaphore.release();
		electionLock.unlock();
	}
}
