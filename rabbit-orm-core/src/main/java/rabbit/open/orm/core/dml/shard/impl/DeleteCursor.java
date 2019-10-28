package rabbit.open.orm.core.dml.shard.impl;

import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.Delete;
import rabbit.open.orm.core.dml.shard.Cursor;

/**
 * 
 * <b>@description 分片删除器 </b>
 * @param <T>
 */
public class DeleteCursor<T> extends Cursor<T> {

	private Delete<T> delete;

	public DeleteCursor(Delete<T> delete) {
		super();
		this.delete = delete;
	}

	@Override
	protected long getAffectedCount() {
		return delete.execute();
	}

	@Override
	protected DMLObject<T> getDMLObject() {
		return delete;
	}

}
