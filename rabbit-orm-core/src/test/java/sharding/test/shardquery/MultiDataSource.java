package sharding.test.shardquery;

import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.core.dml.CombinedDataSource;
import rabbit.open.orm.core.dml.meta.TableMeta;

import javax.sql.DataSource;
import java.util.List;

public class MultiDataSource implements CombinedDataSource {

	List<DataSource> sources;
	
	@Override
	public DataSource getDataSource(Class<?> entityClz, TableMeta meta, DMLType type) {
		return meta.getDataSource();
	}

	@Override
	public List<DataSource> getAllDataSources() {
		return sources;
	}
	
	public void setSources(List<DataSource> sources) {
		this.sources = sources;
	}

}
