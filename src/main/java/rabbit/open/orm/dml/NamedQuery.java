package rabbit.open.orm.dml;

import java.util.Comparator;
import java.util.TreeMap;

import rabbit.open.orm.annotation.Relation.FilterType;
import rabbit.open.orm.dml.meta.JoinFilter;
import rabbit.open.orm.dml.xml.NameQuery;
import rabbit.open.orm.dml.xml.SQLParser;
import rabbit.open.orm.exception.UnSupportedMethodException;
import rabbit.open.orm.pool.SessionFactory;

/**
 * <b>Description: 	命名查询对象</b><br>
 * <b>@author</b>	肖乾斌
 * @param <T>
 * 
 */
public class NamedQuery<T> extends AbstractQuery<T> {

	private NameQuery query;
	
	TreeMap<Integer, Object> fieldsValues;
	
	/**
	 * @param fatory
	 * @param clz
	 * @param name		sql名字
	 */
	public NamedQuery(SessionFactory fatory, Class<T> clz, String name) {
		super(fatory, clz);
		query = SQLParser.getNamedQuery(name, clz);
		fieldsValues = new TreeMap<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
	}

	@Override
	protected void createQuerySql() {
		prepareMany2oneFilters();
		createFieldsSql();
		transformFieldsSql();
		appendNameQuery();
		convertSettedFieldValues();
	}

    /**
     * 
     * <b>Description:  把setFieldValue设置的值转换到preparedValues中</b><br>.	
     * 
     */
    private void convertSettedFieldValues() {
        if(fieldsValues.isEmpty()){
            return;
        }
        preparedValues.addAll(fieldsValues.values());
    }

	@Override
	public NamedQuery<T> distinct() {
		super.distinct();
		return this;
	}

	/**
	 * 
	 * <b>Description:    单个设值</b><br>.
	 * @param fieldName
	 * @param value
	 * @return	
	 * 
	 */
	public NamedQuery<T> setParameterValue(String fieldName, Object value){
	    int index = this.query.getFieldIndex(fieldName);
	    fieldsValues.put(index, value);
	    return this;
	}
	
	
	@Override
	public <E> NamedQuery<T> joinFetch(Class<E> entity) {
		super.joinFetch(entity);
		return this;
	}
	
	@Override
	public NamedQuery<T> fetch(Class<?> clz, Class<?>... dependency) {
		super.fetch(clz, dependency);
		return this;
	}
	
	@Override
	public NamedQuery<T> alias(Class<?> entityClz, String alias) {
		super.alias(entityClz, alias);
		return this;
	}
	
	/**
	 * 
	 * <b>Description:	添加from以后的片段到sql中</b><br>	
	 * 
	 */
	private void appendNameQuery() {
		sql.append(" " + query.getSql().trim());
	}
	
	@Override
	public long count() {
		throw new UnSupportedMethodException(getCurrentMethodName());
	}
	
	@Override
	public NamedQuery<T> addInnerJoinFilter(JoinFilter filter) {
	    throw new UnSupportedMethodException(getCurrentMethodName());
	}

    private String getCurrentMethodName() {
        return Thread.currentThread().getStackTrace()[2].getMethodName();
    }

    @Override
    public NamedQuery<T> addFilter(String reg, Object value, FilterType ft,
            Class<?>... depsPath) {
        throw new UnSupportedMethodException(getCurrentMethodName());
    }

    @Override
    public NamedQuery<T> addFilter(String reg, Object value,
            Class<?>... depsPath) {
        throw new UnSupportedMethodException(getCurrentMethodName());
    }

    @Override
    public NamedQuery<T> addNullFilter(String reg, boolean isNull,
            Class<?>... depsPath) {
        throw new UnSupportedMethodException(getCurrentMethodName());
    }

    @Override
    public NamedQuery<T> addNullFilter(String reg, Class<?>... depsPath) {
        throw new UnSupportedMethodException(getCurrentMethodName());
    }

    @Override
    public NamedQuery<T> addJoinFilter(String reg, FilterType ft,
            Object value, Class<?> target) {
        throw new UnSupportedMethodException(getCurrentMethodName());
    }

    @Override
    public NamedQuery<T> addJoinFilter(String reg, Object value,
            Class<?> target) {
        throw new UnSupportedMethodException(getCurrentMethodName());
    }

    @Override
    public NamedQuery<T> addInnerJoinFilter(String reg, FilterType ft,
            Object value, Class<?> target) {
        throw new UnSupportedMethodException(getCurrentMethodName());
    }

    @Override
    public NamedQuery<T> addInnerJoinFilter(String reg, Object value,
            Class<?> target) {
        throw new UnSupportedMethodException(getCurrentMethodName());
    }
}
