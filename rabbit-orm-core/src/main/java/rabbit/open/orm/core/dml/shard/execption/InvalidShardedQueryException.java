package rabbit.open.orm.core.dml.shard.execption;

import rabbit.open.orm.common.exception.RabbitDMLException;

/**
 * 
 * <b>@description 在非分区表上执行ShardedQuery时抛出此异常  </b>
 */
@SuppressWarnings("serial")
public class InvalidShardedQueryException extends RabbitDMLException {

	public InvalidShardedQueryException(String tableName) {
		super("ShardedQuery is not suitable for table[" + tableName + "]");
	}

}
