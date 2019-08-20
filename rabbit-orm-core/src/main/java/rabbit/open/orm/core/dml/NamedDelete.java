package rabbit.open.orm.core.dml;

import rabbit.open.orm.common.dml.DMLType;

/**
 * <b>@description 命名删除 </b>
 * @param <T>
 */
public class NamedDelete<T> extends NamedUpdate<T> {

	public NamedDelete(SessionFactory factory, Class<T> clz, String name) {
		super(factory, clz, name, DMLType.DELETE);
	}

}
