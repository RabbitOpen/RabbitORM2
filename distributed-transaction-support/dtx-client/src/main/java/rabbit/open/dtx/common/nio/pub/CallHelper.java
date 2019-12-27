package rabbit.open.dtx.common.nio.pub;

import java.io.IOException;

/**
 * 调用工具
 * @author xiaoqianbin
 * @date 2019/12/27
 **/
public class CallHelper {

    /**
     * 不关心异常的调用，除非你确认这个异常不会发生。或者发生了也不用处理才调用这个方法
     * @param    r
     * @author xiaoqianbin
     * @date 2019/12/23
     **/
    public static void ignoreExceptionCall(Callback r) {
        try {
            r.execute();
        } catch (Exception e) {
            // to do ignore
        }
    }

    public interface Callback {
        void execute() throws InterruptedException, IOException;
    }
}
