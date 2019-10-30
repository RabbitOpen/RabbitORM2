package rabbit.open.orm.core.dml.policy;

/**
 * <b>@description 分页策略 </b>
 */
public enum PagePolicy {

	UNIQUE_INDEX_ORDERED("通过唯一索引排序"),
	
	DEFAULT("默认分页策略");
	
	PagePolicy(String desc) {}
}
