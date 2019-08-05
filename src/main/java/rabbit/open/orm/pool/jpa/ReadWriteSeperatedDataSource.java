package rabbit.open.orm.pool.jpa;

import rabbit.open.orm.dml.filter.DMLType;
import rabbit.open.orm.pool.SessionFactory;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;

/**
 * <b>Description  读写分离数据源</b>
 */
public class ReadWriteSeperatedDataSource implements CombinedDataSource {

    //读的源
    private DataSource readSource;

    //写的源
    private DataSource writeSource;

    private List<DataSource> sources = new ArrayList<>();
    
    @Override
    public DataSource getDataSource(Class<?> entityClz, String tableName, DMLType type) {
    	if (SessionFactory.isTransactionOpen()) {
    		return getWriteSource();
    	}
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
        sources.add(readSource);
    }
    
    public void setWriteSource(DataSource writeSource) {
        this.writeSource = writeSource;
        sources.add(writeSource);
    }

	@Override
	public List<DataSource> getAllDataSources() {
		return sources;
	}
}
