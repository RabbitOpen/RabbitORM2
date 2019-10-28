package rabbit.open.orm.core.dml.shard.impl;

import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.Update;
import rabbit.open.orm.core.dml.shard.Cursor;

/**
 * 
 * <b>@description 分片更新器 </b>
 * @param <T>
 */
public class UpdateCursor<T> extends Cursor<T> {

	private Update<T> update;

	public UpdateCursor(Update<T> delete) {
		super();
		this.update = delete;
	}

	@Override
	protected long getAffectedCount() {
		return update.execute();
	}

	@Override
	protected DMLObject<T> getDMLObject() {
		return update;
	}

}
