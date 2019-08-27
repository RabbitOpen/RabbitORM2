package rabbit.open.orm.core.spring;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;

import org.apache.log4j.Logger;

import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.dml.DMLAdapter;
import rabbit.open.orm.core.dml.Delete;
import rabbit.open.orm.core.dml.DynamicQuery;
import rabbit.open.orm.core.dml.Insert;
import rabbit.open.orm.core.dml.JoinTableManager;
import rabbit.open.orm.core.dml.NamedDelete;
import rabbit.open.orm.core.dml.NamedQuery;
import rabbit.open.orm.core.dml.NamedUpdate;
import rabbit.open.orm.core.dml.Query;
import rabbit.open.orm.core.dml.SQLQuery;
import rabbit.open.orm.core.dml.SessionFactory;
import rabbit.open.orm.core.dml.Update;
import rabbit.open.orm.core.dml.meta.MetaData;

/**
 * <b>Description: 	database access object</b><br>
 * <b>@author</b>	肖乾斌
 * @param <T>
 * 
 */
public abstract class SpringDaoAdapter<T> {

	protected Logger logger = Logger.getLogger(getClass());
	
	protected Class<T> clz;
	
	protected SessionFactory sessionFactory;
	
	@SuppressWarnings("unchecked")
    public SpringDaoAdapter() {
        try {
        	Class<?> cls = getClass();
        	while (!(cls.getGenericSuperclass() instanceof ParameterizedType)) {
        		cls = cls.getSuperclass();
        	}
            this.clz = (Class<T>) ((ParameterizedType) (cls.getGenericSuperclass())).getActualTypeArguments()[0];
        } catch (Exception e) {

        }
    }

	public void setSessionFactory(SessionFactory sessionFactory) {
        this.sessionFactory = sessionFactory;
    }
	
	/**
	 * 
	 * <b>Description:	创建一个查询对象</b><br>
	 * @return	
	 * 
	 */
	public Query<T> createQuery() {
		return new Query<>(sessionFactory, clz);
	}

	/**
	 * 
	 * <b>Description:	创建一个查询对象</b><br>
	 * @param filter	原始过滤条件
	 * @return	
	 * 
	 */
	public Query<T> createQuery(T filter) {
		return new Query<>(sessionFactory, filter, clz);
	}
	
	/**
	 * 
	 * <b>Description:	创建一个可以查询动态字段的查询对象</b><br>
	 * @return	
	 * 
	 */
	public Query<T> createDynamicQuery() {
		return new DynamicQuery<>(sessionFactory, clz);
	}
	
	/**
	 * 
	 * <b>Description:	创建一个可以查询动态字段的查询对象</b><br>
	 * @param filter	原始过滤条件
	 * @return	
	 * 
	 */
	public Query<T> createDynamicQuery(T filter) {
		return new DynamicQuery<>(sessionFactory, filter, clz);
	}
	
	/**
	 * 
	 * <b>Description:	新增一条数据</b><br>
	 * @param data
	 * @return	
	 * 
	 */
	public long add(T data) {
		return new Insert<>(sessionFactory, clz, data).execute();
	}

	/**
	 * 
	 * <b>Description:	创建一个sql查询对象</b><br>
	 * @param sqlName	查询的name信息
	 * @return	
	 * 
	 */
	public NamedQuery<T> createNamedQuery(String sqlName) {
		return new NamedQuery<>(sessionFactory, clz, sqlName);
	}

	/**
	 * <b>@description  创建一个命名更新 </b>
	 * @param sqlName  命名更新的名字
	 * @return
	 */
	public NamedUpdate<T> createNamedUpdate(String sqlName) {
		return new NamedUpdate<>(sessionFactory, clz, sqlName);
	}
	
	/**
	 * <b>@description  创建一个命名删除</b>
	 * @param sqlName  命名删除的名字
	 * @return
	 */
	public NamedDelete<T> createNamedDelete(String sqlName) {
		return new NamedDelete<>(sessionFactory, clz, sqlName);
	}
	
	
	/**
	 * 
	 * <b>Description:	创建一个sql查询</b><br>
	 * @param sqlName
	 * @return	
	 * 
	 */
	public SQLQuery<T> createSQLQuery(String sqlName) {
		return new SQLQuery<>(sessionFactory, clz, sqlName);
	}
	
	/**
	 * 
	 * <b>Description:	创建一个带过滤条件的更新对象</b><br>
	 * @param filterData
	 * @return	
	 * 
	 */
	public Update<T> createUpdate(T filterData) {
		return new Update<>(sessionFactory, filterData, clz);
	}

	/**
	 * 
	 * <b>Description:	创建一个更新对象</b><br>
	 * @return	
	 * 
	 */
	public Update<T> createUpdate() {
		return new Update<>(sessionFactory, clz);
	}
	
	/**
	 * 
	 * <b>Description:	根据主键进行更新</b><br>
	 * @param data
	 * @return
	 * 
	 */
	public long updateByID(T data) {
		return new Update<>(sessionFactory, clz).updateByID(data);
	}

	/**
	 * <b>@description 根据id替换数据库中的同id数据  </b>
	 * @param data
	 * @return
	 */
	public long replaceByID(T data) {
		return new Update<>(sessionFactory, clz).replaceByID(data);
	}
	
	/**
	 * 
	 * <b>Description:	新增中间表数据</b><br>
	 * @param data	
	 * 
	 */
	public void addJoinRecords(T data) {
		new JoinTableManager<>(sessionFactory, clz).addJoinRecords(data);
	}
	
	/**
	 * 
	 * <b>Description:	删除中间表数据</b><br>
	 * @param data	
	 * 
	 */
	public void removeJoinRecords(T data) {
		new JoinTableManager<>(sessionFactory, clz).removeJoinRecords(data);
	}
	
	/**
	 * 
	 * <b>Description:	清除特定记录的中间表数据</b><br>
	 * @param data		该对象的主键字段必须有值
	 * @param joinClass	
	 * 
	 */
	public void clearJoinRecords(T data, Class<?> joinClass) {
		new JoinTableManager<>(sessionFactory, clz).clearJoinRecords(data, joinClass);
	}
	
	/**
	 * 
	 * <b>Description:	替换中间表数据</b><br>
	 * @param data	
	 * 
	 */
	public void replaceJoinRecords(T data) {
		new JoinTableManager<>(sessionFactory, clz).replaceJoinRecords(data);
	}
	
	/**
	 * 
	 * <b>Description:	根据id查询</b><br>
	 * @param id
	 * @return	
	 * 
	 */
	public T getByID(Serializable id) {
		Query<T> query = new Query<>(sessionFactory, clz);
		return query.addFilter(MetaData.getPrimaryKeyField(clz).getName(), id).execute().unique();
	}
	
	/**
	 * 
	 * <b>Description:	分页查询</b><br>
	 * @param filterData
	 * @param pageIndex
	 * @param pageSize
	 * @return	
	 * 
	 */
	public List<T> query(T filterData, int pageIndex, int pageSize) {
		return createQuery(filterData).page(pageIndex, pageSize).execute().list();
	}

	/**
	 * 
	 * <b>Description:	分页查询</b><br>
	 * @param pageIndex
	 * @param pageSize
	 * @return	
	 * 
	 */
	public List<T> query(int pageIndex, int pageSize) {
		return createQuery().page(pageIndex, pageSize).execute().list();
	}
	
	/**
	 * 
	 * <b>Description:		删除满足data条件的数据</b><br>
	 * @param filterData	该对象中不为null的字段就是删除数据时的过滤条件
	 * @return 				删除的条数
	 * 
	 */
	public long delete(T filterData) {
		Delete<T> delete = new Delete<>(sessionFactory, filterData, clz);
		return delete.execute();
	}

	/**
	 * 
	 * <b>Description:	创建一个带过滤条件的删除对象</b><br>
	 * @param filterData
	 * @return	
	 * 
	 */
	public Delete<T> createDelete(T filterData) {
		return new Delete<>(sessionFactory, filterData, clz);
	}

	/**
	 * 
	 * <b>Description:	创建一个删除对象</b><br>
	 * @return	
	 * 
	 */
	public Delete<T> createDelete() {
		return new Delete<>(sessionFactory, clz);
	}
	
	/**
	 * 
	 * <b>Description:	删除表中所有数据</b><br>
	 * @return 			删除的条数
	 * 
	 */
	public long clearAll() {
		Delete<T> delete = new Delete<>(sessionFactory, clz);
		return delete.execute();
	}
	
	/**
	 * 
	 * <b>Description:	根据主键删除数据</b><br>
	 * @param id
	 * @return	
	 * 
	 */
	public long deleteByID(Serializable id) {
		Delete<T> delete = new Delete<>(sessionFactory, clz);
		return delete.deleteById(id);
	}
	
    /**
     * 
     * <b>Description:  创建根据字段名字映射的查询对象</b><br>.
     *                  支持无限深度内嵌映射
     * @param filterData
     * @return	
     * 
     */
    public Query<T> createFieldsMappingQuery(Object filterData) {
        if (null == filterData) {
            return createQuery();
        }
        T tf = DMLAdapter.newInstance(clz);
        cloneValueByFieldName(filterData, tf);
        return createQuery(tf);
    }

	/**
	 * 
	 * <b>Description:	根据名字复制字段的值, 从src复制到dest</b><br>
	 * @param src
	 * @param dest	
	 * 
	 */
	private final void cloneValueByFieldName(Object src, Object dest) {
		for (Field f : src.getClass().getDeclaredFields()) {
			Object fv = getValue(src, f);
			if (null == fv) {
				continue;
			}
			cloneFieldValue(dest, f, fv);
		}
	}

    private void cloneFieldValue(Object dest, Field field, Object fieldValue) {
		try {
			Field df = dest.getClass().getDeclaredField(field.getName());
			df.setAccessible(true);
			if (df.getType().equals(field.getType())) {
				df.set(dest, fieldValue);
				return;
			}
			// 类型不匹配
			if (df.getType().getName().startsWith("java")
					|| field.getType().getName().startsWith("java")) {
				return;
			}
			cloneBeanField(dest, fieldValue, df);
		} catch (Exception e) {

		}
    }

    private void cloneBeanField(Object dest, Object value, Field field) {
        Object clone = DMLAdapter.newInstance(field.getType());
        cloneValueByFieldName(value, clone);
        setValue(dest, field, clone);
    }

    private void setValue(Object obj, Field field, Object value) {
        try {
            field.set(obj, value);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RabbitDMLException(e);
        }
    }

    private Object getValue(Object src, Field f) {
        try {
            f.setAccessible(true);
            return f.get(src);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            throw new RabbitDMLException(e);
        }
    }

}
