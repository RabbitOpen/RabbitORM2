package rabbit.open.dtx.common.utils;

/**
 * 节点id辅助类
 * @author xiaoqianbin
 * @date 2019/12/30
 **/
public class NodeIdHelper {

    private NodeIdHelper() {}

    /**
     * 生成19位的同网段唯一服务器id
     * @param	hostAddress
     * @param	port
     * @author  xiaoqianbin
     * @date    2019/12/23
     **/
    public static String calcServerId(String hostAddress, int port) {
        String[] segments = hostAddress.split("\\.");
        StringBuilder id = new StringBuilder();
        for (String seg : segments) {
            id.append(String.format("%03d", Integer.parseInt(seg)));
        }
        id.append(String.format("%07d", port));
        return id.toString();
    }
}
