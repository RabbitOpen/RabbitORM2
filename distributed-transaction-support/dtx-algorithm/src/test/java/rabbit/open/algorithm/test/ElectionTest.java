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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 单元测试
 * @author xiaoqianbin
 * @date 2019/12/30
 **/
@RunWith(JUnit4.class)
public class ElectionTest {

    static ElectionArbiter leaderArbiter;

    /**
     * 普通投票/重投测试
     * @author  xiaoqianbin
     * @date    2020/1/14
     **/
    @Test
    public void voteTest() throws InterruptedException {
        leaderArbiter = null;
        MockPostman.postmen.clear();
        int count = 5;
        Semaphore semaphore = new Semaphore(0);
        List<ElectionArbiter> arbiters = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            MockPostman postman = new MockPostman();
            ElectionArbiter arbiter = new ElectionArbiter(count, "node-" + i, new LeaderElectedListener() {

                @Override
                public void onLeaderElected(ElectionArbiter node) {
                    super.onLeaderElected(node);
                    leaderArbiter = node;
                    try {
                        holdOn(100);
                    } catch (Exception e) {
                        // to do : ignore
                    }
                    semaphore.release();
                }
            });
            arbiters.add(arbiter);
            postman.register(arbiter);
        }
        for (ElectionArbiter arbiter : arbiters) {
            arbiter.startElection();
        }
        semaphore.acquire();
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
//         调整检测周期
        for (ElectionArbiter arbiter : arbiters) {
            arbiter.setKeepAliveCheckingInterval(1L);
            arbiter.interrupt();
        }
        semaphore.acquire();
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
        semaphore.acquire();
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

    private void holdOn(long milliSeconds) throws InterruptedException {
        Semaphore semaphore = new Semaphore(0);
        if (semaphore.tryAcquire(milliSeconds, TimeUnit.MILLISECONDS)) {
            // 等100ms，保证response已经回来了
            throw new RuntimeException("见鬼了");
        }
    }

    /**
     * 干扰测试
     * @author  xiaoqianbin
     * @date    2020/1/14
     **/
    @Test
    public void disturbTest() throws InterruptedException, NoSuchFieldException, IllegalAccessException {
        leaderArbiter = null;
        MockPostman.postmen.clear();
        int count = 5;
        Semaphore semaphore = new Semaphore(0);
        Semaphore preselectionSemaphore = new Semaphore(0);
        List<ElectionArbiter> arbiters = new ArrayList<>();
        for (int i = 0; i < count - 1; i++) {
            MockPostman postman = new MockPostman();
            ElectionArbiter arbiter = new ElectionArbiter(count, "node-" + i, new LeaderElectedListener() {

                @Override
                public void onLeaderElected(ElectionArbiter node) {
                    super.onLeaderElected(node);
                    leaderArbiter = node;
                    try {
                        holdOn(100);
                    } catch (Exception e) {
                        // to do : ignore
                    }
                    semaphore.release();
                }
            }) {
                @Override
                protected void startPreselectListener() {
                    super.startPreselectListener();
                    preselectionSemaphore.release();
                }
            };
            arbiters.add(arbiter);
            postman.register(arbiter);
        }
        for (int i = 0; i < count - 1; i++) {
            arbiters.get(i).startElection();
        }
        preselectionSemaphore.acquire();
        MockPostman postman = new MockPostman();
        ElectionArbiter arbiter = new ElectionArbiter(count, "node-" + 4, new LeaderElectedListener() {

            @Override
            public void onLeaderElected(ElectionArbiter node) {
                super.onLeaderElected(node);
                leaderArbiter = node;
                try {
                    holdOn(100);
                } catch (Exception e) {
                    // to do : ignore
                }
                semaphore.release();
            }
        });
        arbiters.add(arbiter);
        postman.register(arbiter);
        Field field = ElectionArbiter.class.getDeclaredField("electionPacketVersion");
        field.setAccessible(true);
        AtomicLong electionPacketVersion = (AtomicLong) field.get(arbiter);
        electionPacketVersion.set(100);
        arbiter.startElection();

        semaphore.acquire();

        int leader = 0;
        int follower = 0;
        for (ElectionArbiter electionArbiter : arbiters) {
            if (electionArbiter.getNodeRole() == NodeRole.LEADER) {
                leader++;
            }
            if (electionArbiter.getNodeRole() == NodeRole.FOLLOWER) {
                follower++;
            }
        }
        for (ElectionArbiter electionArbiter : arbiters) {
            electionArbiter.shutdown();
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
