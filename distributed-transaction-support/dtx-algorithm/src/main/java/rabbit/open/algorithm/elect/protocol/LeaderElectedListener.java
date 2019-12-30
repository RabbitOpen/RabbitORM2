package rabbit.open.algorithm.elect.protocol;

import rabbit.open.algorithm.elect.event.ElectionEventListener;

/**
 * leader 成功选举事件
 * @author xiaoqianbin
 * @date 2019/12/30
 **/
public class LeaderElectedListener implements ElectionEventListener {

    @Override
    public void onElectionEnd(ElectionArbiter arbiter) {
        arbiter.startKeepAliveThread();
    }


}
