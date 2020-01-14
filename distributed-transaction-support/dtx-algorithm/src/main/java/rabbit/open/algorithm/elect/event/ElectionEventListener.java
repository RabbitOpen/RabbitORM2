package rabbit.open.algorithm.elect.event;

import rabbit.open.algorithm.elect.protocol.ElectionArbiter;

/**
 * 选举事件
 * @author xiaoqianbin
 * @date 2019/12/30
 **/
public interface ElectionEventListener {

    /**
     * leader选举出来了
     * @param   arbiter     当前节点
     * @author  xiaoqianbin
     * @date    2019/12/30
     **/
    void onLeaderElected(ElectionArbiter arbiter);
}
