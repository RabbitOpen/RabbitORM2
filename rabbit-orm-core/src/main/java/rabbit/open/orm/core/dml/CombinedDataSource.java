package rabbit.open.orm.core.dml;

import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.core.dml.meta.TableMeta;

import javax.sql.DataSource;
import java.util.List;

/**
 * <b>Description  复合数据源接口，主要对分表分库进行支撑</b>
 */
public interface CombinedDataSource {

    /**
     * <b>Description       DML操作时, 获取数据源</b>
     * @param entityClz     实体类
     * @param tableMeta     当前操作的表meta
     * @param type          INSERT, DELETE, SELECT, UPDATE
     * @return              数据源
     */
    public abstract DataSource getDataSource(Class<?> entityClz, TableMeta tableMeta, DMLType type);
    
    /**
     * <b>@description  获取所有数据源</b>
     * @return
     */
    public abstract List<DataSource> getAllDataSources();

}
