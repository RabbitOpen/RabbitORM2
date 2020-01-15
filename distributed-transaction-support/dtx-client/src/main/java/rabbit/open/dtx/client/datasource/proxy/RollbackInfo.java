package rabbit.open.dtx.client.datasource.proxy;

import rabbit.open.dtx.client.datasource.parser.ColumnMeta;
import rabbit.open.dtx.client.datasource.parser.SQLMeta;
import rabbit.open.dtx.common.annotation.RollbackPolicy;

import java.util.List;
import java.util.Map;

/**
 * 回滚信息
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class RollbackInfo {

    private SQLMeta meta;

    private List<Object> preparedValues;

    // 上个版本的数据信息
    private List<Map<String, Object>> originalData;

    // 自增列meta
    private ColumnMeta autoIncrementColumnMeta;

    private RollbackPolicy rollbackPolicy;

    public RollbackInfo() {
    }

    public RollbackInfo(SQLMeta meta, List<Object> preparedValues) {
        this(meta, preparedValues, null);
    }

    public RollbackInfo(SQLMeta meta, List<Object> preparedValues, List<Map<String, Object>> originalData) {
        this.meta = meta;
        this.preparedValues = preparedValues;
        this.originalData = originalData;
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

    public List<Map<String, Object>> getOriginalData() {
        return originalData;
    }

    public void setOriginalData(List<Map<String, Object>> originalData) {
        this.originalData = originalData;
    }

    public ColumnMeta getAutoIncrementColumnMeta() {
        return autoIncrementColumnMeta;
    }

    public void setAutoIncrementColumnMeta(ColumnMeta autoIncrementColumnMeta) {
        this.autoIncrementColumnMeta = autoIncrementColumnMeta;
    }

    public RollbackPolicy getRollbackPolicy() {
        return rollbackPolicy;
    }

    public void setRollbackPolicy(RollbackPolicy rollbackPolicy) {
        this.rollbackPolicy = rollbackPolicy;
    }
}
