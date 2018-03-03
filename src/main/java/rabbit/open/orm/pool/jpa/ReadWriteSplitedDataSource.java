package rabbit.open.orm.pool.jpa;

import javax.sql.DataSource;

import rabbit.open.orm.dml.filter.DMLType;

/**
 * <b>Description  读写分离数据源</b>
 */
public class ReadWriteSplitedDataSource implements CombinedDataSource {

    //写的源
    private DataSource readSource;

    //读的源
    private DataSource writeSource;
    
    @Override
    public DataSource getDataSource(Class<?> entityClz, String tableName, DMLType type) {
        if(DMLType.SELECT.equals(type)){
            return getReadSource();
        }
        return getWriteSource();
    }

    public DataSource getReadSource() {
        return readSource;
    }
    
    public DataSource getWriteSource() {
        return writeSource;
    }
    
    public void setReadSource(DataSource readSource) {
        this.readSource = readSource;
    }
    
    public void setWriteSource(DataSource writeSource) {
        this.writeSource = writeSource;
    }
}
