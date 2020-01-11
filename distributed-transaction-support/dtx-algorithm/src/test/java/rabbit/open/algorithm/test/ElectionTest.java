package rabbit.open.algorithm.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import rabbit.open.algorithm.elect.data.NodeRole;
import rabbit.open.algorithm.elect.data.ProtocolPacket;
import rabbit.open.algorithm.elect.protocol.ElectionArbiter;
import rabbit.open.algorithm.elect.protocol.LeaderElectedListener;
import rabbit.open.algorithm.elect.protocol.Postman;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * 单元测试
 * @author xiaoqianbin
 * @date 2019/12/30
 **/
@RunWith(JUnit4.class)
public class ElectionTest {

    static ElectionArbiter leaderArbiter;

    @Test
    public void voteTest() throws InterruptedException {
        int count = 5;
        Semaphore semaphore = new Semaphore(0);
        List<ElectionArbiter> arbiters = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MockPostman postman = new MockPostman();
            ElectionArbiter arbiter = new ElectionArbiter(count, "node-" + i, new LeaderElectedListener() {

                @Override
                public void onElectionEnd(ElectionArbiter node) {
                    super.onElectionEnd(node);
                    if (node.getNodeRole() == NodeRole.LEADER) {
                        semaphore.release(2);
                        leaderArbiter = node;
                    } else if (node.getNodeRole() == NodeRole.FOLLOWER) {
                        semaphore.release(1);
                    }
                }
            });
            arbiters.add(arbiter);
            postman.register(arbiter);
        }
        for (ElectionArbiter arbiter : arbiters) {
            arbiter.startElection();
        }
        semaphore.acquire(count + 1);
        int leader = 0;
        int follower = 0;
        for (ElectionArbiter arbiter : arbiters) {
            if (arbiter.getNodeRole() == NodeRole.LEADER) {
                leader++;
            }
            if (arbiter.getNodeRole() == NodeRole.FOLLOWER) {
                follower++;
            }
        }
        TestCase.assertEquals(1, leader);
        TestCase.assertEquals(arbiters.size() - 1, follower);
        
        
        // =========模拟leader down了，重新选举
        // 调整leader的角色
        leaderArbiter.setNodeRole(NodeRole.FOLLOWER);
        leaderArbiter.stopKeepAlive();
        // 调整检测周期
        for (ElectionArbiter arbiter : arbiters) {
            arbiter.setKeepAliveCheckingInterval(1L);
            arbiter.interrupt();
        }
        semaphore.acquire(count + 1);
        leader = 0;
        follower = 0;
        for (ElectionArbiter arbiter : arbiters) {
            if (arbiter.getNodeRole() == NodeRole.LEADER) {
                leader++;
            }
            if (arbiter.getNodeRole() == NodeRole.FOLLOWER) {
                follower++;
            }
        }
        TestCase.assertEquals(1, leader);
        TestCase.assertEquals(arbiters.size() - 1, follower);

        // =========模拟leader down了，重新选举
        // 调整leader的角色
        leaderArbiter.stopKeepAlive();
        leaderArbiter.setNodeRole(NodeRole.FOLLOWER);
        // 调整检测周期
        for (ElectionArbiter arbiter : arbiters) {
            arbiter.reelectOnLeaderLost(true);
        }
        semaphore.acquire(count + 1);
        leader = 0;
        follower = 0;
        for (ElectionArbiter arbiter : arbiters) {
            if (arbiter.getNodeRole() == NodeRole.LEADER) {
                leader++;
            }
            if (arbiter.getNodeRole() == NodeRole.FOLLOWER) {
                follower++;
            }
        }
        for (ElectionArbiter arbiter : arbiters) {
            arbiter.shutdown();
        }
        TestCase.assertEquals(1, leader);
        TestCase.assertEquals(arbiters.size() - 1, follower);
    }

    /**
     * 模拟邮递员
     * @author  xiaoqianbin
     * @date    2019/12/30
     **/
    static class MockPostman extends Postman {

        static List<MockPostman> postmen = new ArrayList<>();

        static ThreadLocal<Postman> senderContext = new ThreadLocal<>();

        public MockPostman() {
            postmen.add(this);
        }

        @Override
        public void delivery(ProtocolPacket packet) {
            for (MockPostman postman : postmen) {
                if (postman == this) {
                    continue;
                }
                Postman sender = this;
                new Thread(() -> {
                    senderContext.set(sender);
                    postman.onDataReceived(packet);
                    senderContext.remove();
                }).start();
            }
        }

        @Override
        public void ack(ProtocolPacket packet) {
            Postman postman = senderContext.get();
            new Thread(() -> {
                senderContext.set(postman);
                postman.onDataReceived(packet);
                senderContext.remove();
            }).start();
        }
    }
}
