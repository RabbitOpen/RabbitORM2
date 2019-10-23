package sharding.test.shardquery;

import java.util.List;

import javax.sql.DataSource;

import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.core.dml.CombinedDataSource;
import rabbit.open.orm.datasource.RabbitDataSource;

public class MultiDataSource implements CombinedDataSource {

	List<DataSource> sources;
	
	@Override
	public DataSource getDataSource(Class<?> entityClz, String tableName, DMLType type) {
		String tableIndex = tableName.substring(tableName.length() - 3);
		if (Integer.parseInt(tableIndex) < 5) {
			return sources.stream().filter(ds -> "ds1".equals(((RabbitDataSource)ds).getName())).findFirst().get();
		}
		return sources.stream().filter(ds -> "ds2".equals(((RabbitDataSource)ds).getName())).findFirst().get();
	}

	@Override
	public List<DataSource> getAllDataSources() {
		return sources;
	}
	
	public void setSources(List<DataSource> sources) {
		this.sources = sources;
	}

}
