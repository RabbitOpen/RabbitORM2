package rabbit.open.orm.core.datasource;

import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.core.dml.CombinedDataSource;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.dml.meta.TableMeta;

/**
 * <b>Description 读写分离数据源 </b>
 */
public class ReadWriteSeparatedDataSource implements CombinedDataSource {

	// 读的源
	private DataSource readSource;

	// 写的源
	private DataSource writeSource;

	private List<DataSource> sources = new ArrayList<>();

	@Override
	public DataSource getDataSource(Class<?> entityClz, TableMeta tableMeta, DMLType type) {
		if (SessionFactory.isTransactionOpen()) {
			return getWriteSource();
		}
		if (DMLType.SELECT.equals(type)) {
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
