package rabbit.open.orm.core.dml.filter.ext;

/**
 * <b>@description 一对多过滤器 </b>
 */
public class OneToManyFilter extends ManyToManyFilter {

	public OneToManyFilter(Class<?> entityClz) {
		super(entityClz);
	}

	@Override
	protected StringBuilder createJoinSql() {
		return getQuery().createOTMJoinSql(joinFieldMetaData, !isInner());
	}

}
