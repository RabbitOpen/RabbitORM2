package rabbit.open.orm.core.dml.shard.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rabbit.open.orm.core.dml.Delete;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.dml.meta.MetaData;

/**
 * <b>@description 分片删除  </b>
 * @param <T>
 */
public class ShardedDelete<T> {

	protected Logger logger = LoggerFactory.getLogger(getClass());

	protected Delete<T> delete;

	protected SessionFactory factory;

	protected MetaData<T> metaData;

	public ShardedDelete(SessionFactory factory, Class<T> clz, T filter) {

		
	}
}
