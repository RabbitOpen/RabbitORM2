package rabbit.open.algorithm.elect.protocol;

import java.util.Random;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rabbit.open.algorithm.elect.data.ElectionPacket;
import rabbit.open.algorithm.elect.data.ElectedLeader;
import rabbit.open.algorithm.elect.data.NodeRole;
import rabbit.open.algorithm.elect.data.ElectionResult;

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
	private static final AtomicLong VERSION = new AtomicLong(0);
	
	// 候选人总个数
	private int candidateSize = 1;
	
	// 同意选举包的候选人个数
	private int agreedCandiates = 0;
	
	// 选举的锁
	private ReentrantLock electionLock = new ReentrantLock();
	
	// 节点角色
	private NodeRole role = NodeRole.OBSERVER;
	
	public ElectionArbiter(int candidateSize) {
		super(ElectionArbiter.class.getSimpleName());
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
		logger.info("begin to elect");
		while (true) {
			sendElectionPacket();
			if (randomHoldOn()) {
				break;
			}
		}
		logger.info("election is over");
	}

	/**
	 * <b>@description 随机等待3-4秒 </b>
	 * @return
	 * @throws InterruptedException
	 */
	private boolean randomHoldOn() throws InterruptedException {
		return sleepSemaphore.tryAcquire(3000 + new Random().nextInt(1000), TimeUnit.MILLISECONDS);
	}

	/**
	 * <b>@description 发送选举包 </b>
	 * @return false 选举已经结束
	 * @throws InterruptedException 
	 */
	private void sendElectionPacket() throws InterruptedException {
		try {
			electionLock.lock();
			agreedCandiates = 0;
			// 清空信号量
			sleepSemaphore.acquire(sleepSemaphore.availablePermits());
			if (NodeRole.OBSERVER != role) {
				return;
			}
			postman.delivery(new ElectionPacket(VERSION.addAndGet(1L)));
		} finally {
			electionLock.unlock();
		}
	}

	/**
	 * <b>@description 接收到投票结果 </b>
	 */
	@Override
	public void onElectionResultRecieved(ElectionResult result) {
		if (result.getVersion() < VERSION.get()) {
			return;
		}
		electionLock.lock();
		agreedCandiates++;
		if (agreedCandiates == (candidateSize / 2 + 1)) {
			// 票够了就直接唤醒投票线程, 结束投票
			this.role = NodeRole.LEADER;
			sleepSemaphore.release();
			postman.delivery(new ElectedLeader(VERSION.get()));
		}
		electionLock.unlock();
	}

	@Override
	public void postmanBinded(Postman postman) {
		this.postman = postman;
	}

	/**
	 * <b>@description 接收到选举票 </b>
	 * @param electionPacket
	 */
	@Override
	public void onElectionPacketRecieved(ElectionPacket electionPacket) {
		if (NodeRole.LEADER == this.role) {
			// 告诉他我就是leader
			postman.delivery(new ElectedLeader(VERSION.get()));
		} else {
			if (electionPacket.getVersion() <= VERSION.get()) {
				postman.delivery(new ElectionResult(ElectionResult.REJECT, electionPacket.getVersion()));
			} else {
				postman.delivery(new ElectionResult(ElectionResult.AGREE, electionPacket.getVersion()));
			}
		}
	}

	@Override
	public void onElectedLeaderRecieved(ElectedLeader result) {
		electionLock.lock();
		this.role = NodeRole.FOLLOWER;
		sleepSemaphore.release();
		electionLock.unlock();
	}
}
