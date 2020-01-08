package rabbit.open.algorithm.elect.event;

import rabbit.open.algorithm.elect.protocol.ElectionArbiter;

/**
 * 选举事件
 * @author xiaoqianbin
 * @date 2019/12/30
 **/
public interface ElectionEventListener {

    /**
     * 选举开始
     * @author  xiaoqianbin
     * @date    2019/12/30
     **/
    default void onElectionBegin() {}

    /**
     * 集群节点发生变更
     * @author  xiaoqianbin
     * @date    2020/1/7
     **/
    void onCandidatesChanged();

    /**
     * leader选举出来了
     * @param   arbiter     当前节点
     * @author  xiaoqianbin
     * @date    2019/12/30
     **/
    void onElectionEnd(ElectionArbiter arbiter);
}
