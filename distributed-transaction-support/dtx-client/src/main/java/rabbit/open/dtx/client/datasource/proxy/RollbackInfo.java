package rabbit.open.dtx.client.datasource.proxy;

import rabbit.open.dtx.client.datasource.parser.SQLMeta;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 回滚信息
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class RollbackInfo {

    private SQLMeta meta;

    private List<Object> preparedValues;

    public RollbackInfo() {
    }

    public RollbackInfo(SQLMeta meta, List<Object> preparedValues) {
        this.meta = meta;
        this.preparedValues = preparedValues;
    }

    public SQLMeta getMeta() {
        return meta;
    }

    public void setMeta(SQLMeta meta) {
        this.meta = meta;
    }

    public List<Object> getPreparedValues() {
        return preparedValues;
    }

    public void setPreparedValues(List<Object> preparedValues) {
        this.preparedValues = preparedValues;
    }

    public static void main(String[] args) {
        List<Object> list = new ArrayList<>();
        list.add(new Date());
        list.add("s");
        list.add(10L);
    }
}
