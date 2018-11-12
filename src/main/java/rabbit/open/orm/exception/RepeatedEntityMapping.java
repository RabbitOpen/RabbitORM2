package rabbit.open.orm.exception;

/**
 * <b>@description 重复类映射异常(一个实体表只能映射一个类) </b>
 */
@SuppressWarnings("serial")
public class RepeatedEntityMapping extends RabbitDDLException {

	public RepeatedEntityMapping(Class<?> clz1, Class<?> clz2, String tableName) {
		super("table[" + tableName + "] should be mapped by only one class, rather than mapped by class [" + clz1 + "] and [" + clz2 + "] at the same time!");
	}

}
