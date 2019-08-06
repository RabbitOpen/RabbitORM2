package rabbit.open.orm.core.dml;

import java.util.List;

import javax.sql.DataSource;

import rabbit.open.orm.common.dml.DMLType;

/**
 * <b>Description  复合数据源接口</b>
 */
public interface CombinedDataSource {

    /**
     * <b>Description       DML操作时, 获取数据源</b>
     * @param entityClz     实体类
     * @param tableName     当前操作的表名         
     *                      当操作中间表时该值是主表的名字， 所以中间表应该和主表在一个库
     *                      执行SQLQuery时， tableName 为空
     *                      执行NamedQuery时，tableName 为实体类Entity注解中申明表名
     * @param type          INSERT, DELETE, SELECT, UPDATE
     * @return              数据源
     */
    public abstract DataSource getDataSource(Class<?> entityClz, String tableName, DMLType type);
    
    /**
     * <b>@description  获取所有数据源</b>
     * @return
     */
    public abstract List<DataSource> getAllDataSources();

}
