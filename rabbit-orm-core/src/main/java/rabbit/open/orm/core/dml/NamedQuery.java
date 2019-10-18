package rabbit.open.orm.core.dml;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.common.exception.UnKnownFieldException;
import rabbit.open.orm.core.dml.name.FetcherDescriptor;
import rabbit.open.orm.core.dml.name.JoinFetcherDescriptor;
import rabbit.open.orm.core.dml.name.NamedSQL;

/**
 * <b>Description: 	命名查询对象</b><br>
 * <b>@author</b>	肖乾斌
 * @param <T>
 * 
 */
public class NamedQuery<T> {
    
    private Logger logger = LoggerFactory.getLogger(getClass());
	
	private Query<T> query;	
	
	/**
	 * @param factory
	 * @param clz
	 * @param name		sql名字
	 */
	public NamedQuery(SessionFactory factory, Class<T> clz, String name) {
		query = new Query<T>(factory, clz) {
			@Override
			protected void createQuerySql() {
				reset2PreparedStatus();
				aliasMapping.clear();
				generateQuerySql();
			}

			@Override
			protected void createCountSql() {
				reset2PreparedStatus();
				aliasMapping.clear();
				generateNameCountSql();
			}

			/**
			 * 如果命名sql指定了查询的主表名，就使用sql指定的，否则使用entity注解声明的表名
			 * @return
			 */
			@Override
			protected String getCurrentTableName() {
				return getCurrentTableNameByNamedObject(namedObject);
			}
		};
		query.namedObject = query.getSessionFactory().getQueryByNameAndClass(name, clz);
		query.fieldsValues = new TreeMap<>((o1, o2) -> o1.compareTo(o2));
	}

	protected void generateQuerySql() {
	    setEntityAlias();
	    recursivelyFetchEntities();
	    joinFetchEntities();
	    recursivelyJoinFetchEntities(new ArrayList<>(), getNamedObject().getFetchDescriptors());
	    query.prepareMany2oneFilters();
	    query.createFieldsSql();
	    query.sql = new StringBuilder(getNamedObject().replaceFields(query.sql.toString()));
	    setPreparedValues();
	    query.createPageSql();
	}

	private NamedSQL getNamedObject() {
		return query.namedObject;
	}
	
	private void generateNameCountSql() {
		String copySql = getNamedObject().getSql().toLowerCase();
    	String from = "from";
    	query.sql = new StringBuilder("SELECT COUNT(1) " + getNamedObject().getSql().substring(copySql.indexOf(from)));
		setPreparedValues();
	}

    private void joinFetchEntities() {
		FetchDescriptor<T> buildFetch = query.buildFetch();
		for (JoinFetcherDescriptor jfd : getNamedObject().getJoinFetchDescriptors()) {
			buildFetch.joinFetch(jfd.getEntityClass());
		}
    }

    private void recursivelyJoinFetchEntities(List<Class<?>> deps, List<FetcherDescriptor> fetchDescriptors) {
		for (FetcherDescriptor fd : fetchDescriptors) {
			FetchDescriptor<T> bf = query.buildFetch();
			for (Class<?> dep : deps) {
				bf.fetch(dep);
			}
			bf.fetch(fd.getEntityClass());
			for (JoinFetcherDescriptor jfd : fd.getJoinFetchDescriptors()) {
				bf.joinFetch(jfd.getEntityClass());
			}
			List<Class<?>> copyList = copyList(deps);
			copyList.add(fd.getEntityClass());
			recursivelyJoinFetchEntities(copyList, fd.getFetchDescriptors());
		}
    }

    /**
     * <b>Description  级联取出所有many2one对象</b>
     */
    private void recursivelyFetchEntities() {
        List<Class<?>> deps = new ArrayList<>();
	    deps.add(query.getMetaData().getEntityClz());
	    fetch(getNamedObject().getFetchDescriptors(), deps);
    }

	private void fetch(List<FetcherDescriptor> fetchDescriptors, List<Class<?>> deps) {
		for (FetcherDescriptor fd : fetchDescriptors) {
			query.fetch(fd.getEntityClass(),
					deps.toArray(new Class<?>[deps.size()]));
			List<Class<?>> subDeps = copyList(deps);
			subDeps.add(0, fd.getEntityClass());
			fetch(fd.getFetchDescriptors(), subDeps);
		}
    }

	private <D> List<D> copyList(List<D> list) {
		List<D> newList = new ArrayList<>();
		for (D d : list) {
			newList.add(d);
		}
		return newList;
	}
    
    /**
     * <b>Description  设置别名</b>
     */
    private void setEntityAlias() {
        query.alias(query.getMetaData().getEntityClz(), getNamedObject().getAlias());
	    setFetchTableAlias(getNamedObject().getFetchDescriptors());
	    setJoinFetchTableAlias(getNamedObject().getJoinFetchDescriptors());
    }

    private void setFetchTableAlias(List<FetcherDescriptor> fetchers) {
		for (FetcherDescriptor fd : fetchers) {
			query.alias(fd.getEntityClass(), fd.getAlias());
			setJoinFetchTableAlias(fd.getJoinFetchDescriptors());
			setFetchTableAlias(fd.getFetchDescriptors());
		}
    }

    private void setJoinFetchTableAlias(List<JoinFetcherDescriptor> joinFetchers) {
		for (JoinFetcherDescriptor jfd : joinFetchers) {
			query.alias(jfd.getEntityClass(), jfd.getAlias());
		}
    }
	
	public Result<T> execute() {
		return query.execute();
	}

	public List<T> list() {
	    return execute().list();
	}

	public T unique() {
	    return execute().unique();
	}
	
	public long count() {
	    return query.count();
	}

    /**
     * 
     * <b>Description:  把setFieldValue设置的值转换到preparedValues中</b><br>.	
     * 
     */
    private void setPreparedValues() {
        if (getFieldsValues().isEmpty()) {
            return;
        }
        Collection<PreparedValue> values = getFieldsValues().values();
        for (PreparedValue v : values) {
            query.preparedValues.add(v);
        }
    }
    
    /**
     * <b>Description  通过对象的属性字段设值</b>
     * @param filterObject
     * @return
     */
    public NamedQuery<T> set(Object filterObject) {
        if (null == filterObject) {
            return this;
        }
        Field[] fields = filterObject.getClass().getDeclaredFields();
        for (Field f : fields) {
            Object fv = getValue(f, filterObject);
            if (null == fv) {
                continue;
            }
            try {
                set(f.getName(), fv, f.getName(), filterObject.getClass());
            } catch (UnKnownFieldException e) {
                logger.debug("ignore unknown field");
            }
        }
        return this;
    }
    
    /**
     * 
     * <b>@description 分页 </b>
     * @param pageIndex
     * @param pageSize
     * @return
     */
    public NamedQuery<T> page(int pageIndex, int pageSize) {
    	query.page(pageIndex, pageSize);
    	return this;
    }
    
    private Object getValue(Field field, Object target) {
        try {
            field.setAccessible(true);
            return field.get(target);
        } catch (Exception e) {
            throw new RabbitDMLException(e.getMessage());
        }
    }

	/**
	 * <b>Description      单个设值</b>
	 * @param fieldAlias   字段在sql中的别名
	 * @param value        字段的值
	 * @param fieldName    字段在对应实体中的名字
	 * @param entityClz    字段所属的实体
	 * @return
	 */
	public NamedQuery<T> set(String fieldAlias, Object value, String fieldName, Class<?> entityClz) {
		query.setVariable(fieldAlias, value, fieldName, entityClz);
	    return this;
	}

	private TreeMap<Integer, PreparedValue> getFieldsValues() {
		return query.fieldsValues;
	}

	public void showMaskedPreparedSql() {
	    query.showMaskedPreparedSql();
	}
	
	public void showUnMaskedSql() {
	    query.showUnMaskedSql();
	}
	
}
