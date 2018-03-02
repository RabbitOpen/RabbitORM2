package rabbit.open.orm.pool.jpa;

import javax.sql.DataSource;

import rabbit.open.orm.dml.filter.DMLType;

/**
 * <b>Description  复合数据源接口</b>
 */
public interface CombinedDataSource {

    /**
     * <b>Description       DML操作时, 获取数据源</b>
     * @param entityClz     实体类
     * @param tableName     当前操作的表名
     * @param type
     * @return
     */
    public abstract DataSource getDataSource(Class<?> entityClz, String tableName, DMLType type);

}
