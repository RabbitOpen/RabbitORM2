package rabbit.open.dtx.common.annotation;

/**
 * 回滚策略
 * @author xiaoqianbin
 * @date 2020/1/15
 **/
public enum RollbackPolicy {
    STRICT,    // 数据从A到B，回滚时严格校验，只能从B到A
    LOOSE      // 回滚数据时以主键为依据，不校验非主键字段
}
