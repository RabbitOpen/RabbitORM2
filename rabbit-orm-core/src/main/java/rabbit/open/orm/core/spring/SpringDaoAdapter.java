package rabbit.open.orm.core.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.dml.*;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.shard.impl.ShardedDelete;
import rabbit.open.orm.core.dml.shard.impl.ShardedQuery;
import rabbit.open.orm.core.dml.shard.impl.ShardedSQLQuery;
import rabbit.open.orm.core.dml.shard.impl.ShardedUpdate;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.List;

/**
 * <b>Description: 	database access object</b><br>
 * <b>@author</b>	肖乾斌
 * @param <T>
 * 
 */
public abstract class SpringDaoAdapter<T> {

	protected Logger logger = LoggerFactory.getLogger(getClass());
	
	protected Class<T> clz;
	
	protected SessionFactory sessionFactory;
	
    public SpringDaoAdapter() {
    	this.clz = getTemplateClz(getClass());
    }

    @SuppressWarnings("unchecked")
	public static <D> Class<D> getTemplateClz(Class<?> clz) {
    	Class<?> cls = clz;
    	while (!(cls.getGenericSuperclass() instanceof ParameterizedType)) {
    		cls = cls.getSuperclass();
    	}
    	return (Class<D>) ((ParameterizedType) (cls.getGenericSuperclass())).getActualTypeArguments()[0];
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
	 * 批量添加数据
	 * @param	list
	 * @author  xiaoqianbin
	 * @date    2020/6/1
	 **/
	public long addBatch(List<T> list) {
		if (sessionFactory.getDialectType().isMysql()) {
			return new Insert<>(sessionFactory, clz, list).execute();
		} else {
			if (CollectionUtils.isEmpty(list)) {
				return 0L;
			}
			for (T data : list) {
				add(data);
			}
			return list.size();
		}
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
	 * 
	 */
	public SQLQuery<T> createSQLQuery(String sqlName) {
		return new SQLQuery<>(sessionFactory, clz, sqlName);
	}

	/**
	 * 
	 * <b>Description:	创建一个分片sql查询</b><br>
	 * @param sqlName
	 * 
	 */
	public ShardedSQLQuery<T> createShardedSQLQuery(String sqlName) {
		return new ShardedSQLQuery<>(sessionFactory, clz, sqlName);
	}
	
	/**
	 * 
	 * <b>Description:	创建一个带过滤条件的更新对象</b><br>
	 * @param filterData
	 * 
	 */
	public Update<T> createUpdate(T filterData) {
		return new Update<>(sessionFactory, filterData, clz);
	}

	/**
	 * 
	 * <b>Description:	创建一个更新对象</b><br>
	 * 
	 */
	public Update<T> createUpdate() {
		return new Update<>(sessionFactory, clz);
	}
	
	/**
	 * 
	 * <b>Description:	根据主键进行更新</b><br>
	 * @param data
	 * 
	 */
	public long updateByID(T data) {
		return new Update<>(sessionFactory, clz).updateByID(data);
	}

	/**
	 * <b>@description 根据id替换数据库中的同id数据  </b>
	 * @param data
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
	 * <b>Description:	新增指定的中间表数据</b><br>
	 * @param data	
	 * @param joinClass	
	 * 
	 */
	public void addJoinRecords(T data, Class<?> joinClass) {
		new JoinTableManager<>(sessionFactory, clz).addJoinRecords(data, joinClass);
	}
	
	/**
	 * <b>Description:	删除所有中间表数据</b><br>
	 * @param data	
	 */
	public void removeJoinRecords(T data) {
		new JoinTableManager<>(sessionFactory, clz).removeJoinRecords(data);
	}
	
	/**
	 * <b>Description:	清除特定记录的中间表数据</b><br>
	 * @param data		该对象的主键字段必须有值
	 * @param joinClass	
	 */
	public void removeJoinRecords(T data, Class<?> joinClass) {
		new JoinTableManager<>(sessionFactory, clz).removeJoinRecords(data, joinClass);
	}
	
	/**
	 * 
	 * <b>Description:	替换特定表的中间表数据</b><br>
	 * @param data	
	 * @param joinClass	
	 * 
	 */
	public void replaceJoinRecords(T data, Class<?> joinClass) {
		new JoinTableManager<>(sessionFactory, clz).replaceJoinRecords(data, joinClass);
	}

	/**
	 * 
	 * <b>Description:	合并特定表的中间表数据</b><br>
	 * @param data	
	 * @param joinClass	
	 * 
	 */
	public void mergeJoinRecords(T data, Class<?> joinClass) {
		new JoinTableManager<>(sessionFactory, clz).mergeJoinRecords(data, joinClass);
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
	 * <b>@description 创建一个分片查询 </b>
	 * @param filterData
	 */
	public ShardedQuery<T> createShardedQuery(T filterData) {
		return new ShardedQuery<>(sessionFactory, clz, filterData);
	}

	/**
	 * <b>@description 创建一个分片查询 </b>
	 */
	public ShardedQuery<T> createShardedQuery() {
		return createShardedQuery(null);
	}

	/**
	 * <b>@description 创建一个分片删除 </b>
	 * @param filterData
	 */
	public ShardedDelete<T> createShardedDelete(T filterData) {
		return new ShardedDelete<>(sessionFactory, clz, filterData);
	}
	
	/**
	 * <b>@description 创建一个分片删除  </b>
	 */
	public ShardedDelete<T> createShardedDelete() {
		return createShardedDelete(null);
	}

	/**
	 * <b>@description 创建一个分片更新 </b>
	 * @param filterData
	 */
	public ShardedUpdate<T> createShardedUpdate(T filterData) {
		return new ShardedUpdate<>(sessionFactory, clz, filterData);
	}
	
	/**
	 * <b>@description 创建一个分片更新  </b>
	 */
	public ShardedUpdate<T> createShardedUpdate() {
		return createShardedUpdate(null);
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
        T tf = DMLObject.newInstance(clz);
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
			// TO DO : ignore
		}
    }

    private void cloneBeanField(Object dest, Object value, Field field) {
        Object clone = DMLObject.newInstance(field.getType());
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
