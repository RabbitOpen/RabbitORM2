package rabbit.open.orm.common.exception;

/**
 * <b>Description 不存在的命名查询 </b>
 */
@SuppressWarnings("serial")
public class NamedSQLNotExistedException extends RuntimeException {

	public NamedSQLNotExistedException(String name) {
		super("no named sql[" + name + "] is found");
	}

}
