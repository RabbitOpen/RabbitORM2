package rabbit.open.dtx.common.exception;

/**
 * 没有可用的节点
 * @author xiaoqianbin
 * @date 2020/1/8
 **/
@SuppressWarnings("serial")
public class NoActiveNodeException extends DtxException {

    public NoActiveNodeException() {
        super("no extra active node is available");
    }
}
