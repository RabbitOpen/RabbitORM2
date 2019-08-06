package rabbit.open.orm.dml.meta;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import rabbit.open.common.annotation.Column;
import rabbit.open.common.annotation.Entity;
import rabbit.open.common.annotation.ManyToMany;
import rabbit.open.common.annotation.OneToMany;
import rabbit.open.common.annotation.PrimaryKey;
import rabbit.open.common.exception.RabbitDMLException;
import rabbit.open.common.exception.UnKnownFieldException;
import rabbit.open.common.shard.ShardingPolicy;
import rabbit.open.orm.dml.DMLAdapter;
import rabbit.open.orm.dml.meta.proxy.ManyToManyProxy;
import rabbit.open.orm.dml.meta.proxy.OneToManyProxy;
import rabbit.open.orm.pool.SessionFactory;


/**
 * Table元信息
 * @author	肖乾斌
 * 
 */
public class MetaData<T> {

	//缓存实体表名和实体类的映射关系
	private static ConcurrentHashMap<String, Class<?>> tableMapping = new ConcurrentHashMap<>();

	//缓存实体类和实体表名的映射关系
	private static ConcurrentHashMap<Class<?>, String> clzMapping = new ConcurrentHashMap<>();
	
	//meta信息的缓存
	private static ConcurrentHashMap<Class<?>, MetaData<?>> metaMapping = new ConcurrentHashMap<>();

	//缓存实体类和字段的映射关系
	private static ConcurrentHashMap<Class<?>, Map<String, FieldMetaData>> fieldsMapping = new ConcurrentHashMap<>();
	
	//缓存主键和class的关系
	private static ConcurrentHashMap<Class<?>, Field> primaryKeyMapping = new ConcurrentHashMap<>();
	
	//oracle数据库不区分大小写，所以结果集字段和实体类字段需要映射一下
	private static Map<Class<?>, Map<String, String>> fieldsAlias = new ConcurrentHashMap<>();
	
	//字段元信息
	private Collection<FieldMetaData> fieldMetas;
	
	//能进行一对多、多对多查询的实体信息
	protected List<JoinFieldMetaData<?>> joinMetas;
	
	private ShardingPolicy shardingPolicy;
	
	private Map<Class<? extends ShardingPolicy>, ShardingPolicy> policyMapping = new ConcurrentHashMap<>();
	
	//表名
	protected String tableName;
	
	//主键字段Column信息
	protected Column primaryKey;
		
	//实体对应的类的class信息
	protected Class<T> entityClz;
	
    private MetaData(Class<T> entityClz) {
        this.entityClz = entityClz;
    }

    public static <D> MetaData<?> getMetaByClass(Class<D> clz) {
        if (metaMapping.containsKey(clz)) {
            return metaMapping.get(clz);
        }
        cacheMeta(clz);
        return metaMapping.get(clz);
    }

	/**
	 * <b>@description 缓存meta信息 </b>
	 * @param clz
	 */
	private static synchronized <D> void cacheMeta(Class<D> clz) {
		if (metaMapping.containsKey(clz)) {
			return;
		}
		MetaData<D> meta = new MetaData<>(clz);
		meta.registClassTableMapping(clz);
		metaMapping.put(clz, meta);
	}

	public List<JoinFieldMetaData<?>> getJoinMetas() {
		return joinMetas;
	}

	public String getTableName() {
		return tableName;
	}

	public Column getPrimaryKey() {
		return primaryKey;
	}

	public ShardingPolicy getShardingPolicy() {
        return shardingPolicy;
    }
	
	public static String getPrimaryKey(Class<?> clz, SessionFactory factory) {
	    Column column = getPrimaryKeyFieldMeta(clz).getColumn();
        return factory.getColumnName(column);
	}

	/**
     * 
     * <b>Description:  注册类和表的映射关系</b><br>
     * @param entityClz 
     * 
     */
    private void registClassTableMapping(Class<T> entityClz) {
        tableName = entityClz.getAnnotation(Entity.class).value();
        shardingPolicy = loadShardingPolicy(entityClz);
        primaryKey = getPrimaryKeyFieldMeta(entityClz).getColumn();
        tableMapping.put(tableName, entityClz);
        clzMapping.put(entityClz, tableName);
        joinMetas = getJoinMetas(entityClz);
        fieldMetas = getMappingFieldMetas(entityClz).values();
    }

    private ShardingPolicy loadShardingPolicy(Class<T> entityClz) {
        Entity entity = entityClz.getAnnotation(Entity.class);
        if (policyMapping.containsKey(entity.policy())) {
            return policyMapping.get(entity.policy());
        }
        try {
            policyMapping.put(entity.policy(), DMLAdapter.newInstance(entity.policy()));
        } catch (Exception e) {
            throw new RabbitDMLException(e);
        }
        return policyMapping.get(entity.policy());
    }
	/**
	 * 
	 * 获取类中和表有映射关系的字段
	 * @param clzz
	 * 
	 */
    private static Map<String, FieldMetaData> getMappingFieldMetas(Class<?> clzz) {
        Class<?> clz = clzz;
        Map<String, FieldMetaData> fields = new ConcurrentHashMap<>();
        Map<Class<?>, Integer> counter = new HashMap<>();
        while (!clz.equals(Object.class)) {
            lookupMappingFieldMetasByClass(clz, fields, counter);
            clz = clz.getSuperclass();
        }
        return fields;
    }

    private static void lookupMappingFieldMetasByClass(Class<?> clz,
    		Map<String, FieldMetaData> fields, Map<Class<?>, Integer> counter) {
        for (Field f : clz.getDeclaredFields()) {
            Column col = f.getAnnotation(Column.class);
            if (null == col) {
                continue;
            }
            if (counter.containsKey(f.getType())) {
                counter.put(f.getType(), counter.get(f.getType()) + 1);
            } else {
                counter.put(f.getType(), 1);
            }
            FieldMetaData fmd = new FieldMetaData(f, col);
            fmd.setIndex(counter.get(f.getType()));
            if (fmd.getIndex() > 1) {
                fmd.setMutiFetchField(true);
                flagMutiFetchFieldByType(fields, f.getType());
            }
            fields.put(f.getName(), fmd);
        }
    }

	//标识type对应的字段为mutiFetchField
    private static void flagMutiFetchFieldByType(Map<String, FieldMetaData> fields,
            Class<?> type) {
        for (FieldMetaData fmd : fields.values()) {
            if (fmd.getField().getType().equals(type)) {
                fmd.setMutiFetchField(true);
            }
        }
    }

	public Collection<FieldMetaData> getFieldMetas() {
		return fieldMetas;
	}
	
	/**
	 * 
	 * 查找主键字段
	 * @param clz
	 * @return
	 * 
	 */
	public static FieldMetaData getPrimaryKeyFieldMeta(Class<?> clz) {
		Map<String, FieldMetaData> fmds = getCachedFieldsMetas(clz);
		for (FieldMetaData fmd : fmds.values()) {
			if (fmd.isPrimaryKey()) {
				return fmd;
			}
		}
	    throw new RabbitDMLException("no primary key was found in [" + clz.getName() + "]");
	}
	
	public static Field getPrimaryKeyField(Class<?> clzz) {
		Class<?> clz = clzz;
		if (null != primaryKeyMapping.get(clz)) {
			return primaryKeyMapping.get(clz);
		}
		String name = clz.getName();
		while (true) {
			for (Field f : clz.getDeclaredFields()) {
				PrimaryKey pk = f.getAnnotation(PrimaryKey.class);
				if (null != pk) {
					primaryKeyMapping.put(clz, f);
					return f;
				}
			}
			clz = clz.getSuperclass();
			if (clz.equals(Object.class)) {
				break;
			}
		}
		throw new RabbitDMLException("no primary key was found in [" + name + "]");
	}
	
	//通过类信息查表名
	public static String getTableNameByClass(Class<?> clz) {
		String tableName = clzMapping.get(clz);
		if (null == tableName) {
			Class<?> c = clz;
			while (null == c.getAnnotation(Entity.class)) {
				c = c.getSuperclass();
				if (c == null) {
					return null;
				}
			}
			String tbName = c.getAnnotation(Entity.class).value();
			clzMapping.put(clz, tbName);
			clzMapping.put(c, tbName);
			tableMapping.put(tbName, c);
			return tbName;
		}
		return tableName;
	}
	
	/**
	 * <b>Description  修改表名：   当前表名-->分区表名</b>
	 * @param tableName
	 */
	public void updateTableName(String tableName) {
        this.tableName = tableName;
        tableMapping.put(tableName, entityClz);
    }
	
	/**
	 * 
	 * 判断该clz是否是entity
	 * @param 	clz
	 * 
	 */
	public static boolean isEntityClass(Class<?> clz) {
		return null != clz.getAnnotation(Entity.class);
	}
	
	/**
	 * 
	 * 更新表和类的映射
	 * @param table
	 * @param clz
	 * 
	 */
	public static void updateTableMapping(String table, Class<?> clz) {
		if (tableMapping.containsKey(table)) {
			return;
		}
		tableMapping.put(table, clz);
	}

	public Class<T> getEntityClz() {
		return entityClz;
	}
	
	/**
	 * 
	 * 获取clz中表字段的映射信息
	 * @param clz
	 * @return
	 * @throws Exception 
	 * 
	 */
	public static Map<String, FieldMetaData> getCachedFieldsMetas(Class<?> clz) {
		Map<String, FieldMetaData> mapping = fieldsMapping.get(clz);
        if (null != mapping) {
            return mapping;
        } else {
            Class<?> c = clz;
            while (!c.equals(Object.class)) {
                c = c.getSuperclass();
                if (null != fieldsMapping.get(c)) {
                    return fieldsMapping.get(c);
                }
            }
            mapping = getMappingFieldMetas(clz);
            fieldsMapping.put(clz, mapping);
        }
        return mapping;
	}
	
	/**
	 * <b>Description  获取clz中指定字段类型的FieldMetaData.</b>
	 * @param clz
	 * @param type
	 * @return
	 */
	public static FieldMetaData getCachedFieldMetaByType(Class<?> clz,
			Class<?> type) {
        Map<String, FieldMetaData> cachedFieldsMetas = getCachedFieldsMetas(clz);
        for (FieldMetaData fmd : cachedFieldsMetas.values()) {
            if (fmd.getField().getType().equals(type)) {
                return fmd;
            }
        }
        throw new RabbitDMLException(type + " doesn't belong to " + clz);
	}
	
	/**
	 * 
	 * <b>Description:	获取{fieldName}的FieldMetaData信息</b><br>
	 * @param clz
	 * @param fieldName
	 * @return	
	 * 
	 */
	public static FieldMetaData getCachedFieldsMeta(Class<?> clz,
			String fieldName) {
		Map<String, FieldMetaData> cachedFieldsMetas = getCachedFieldsMetas(clz);
		if (cachedFieldsMetas.containsKey(fieldName)) {
			return cachedFieldsMetas.get(fieldName);
		}
		throw new UnKnownFieldException("field[" + fieldName + "] does not belong to class[" 
                + clz.getName() + "]");
	}
	
	//字段别名映射 key 是别名 value是字段名
	public static Map<String, String> getFieldsAliasMapping(Class<?> clz) {
		return fieldsAlias.get(clz);
	}
	
	public static void setFieldsAliasMapping(Class<?> clz,
			Map<String, String> mapping) {
		fieldsAlias.put(clz, mapping);
	}
		
	/**
	 * 
	 * <b>Description:	获取能够被一对多，多对多查询的字段的meta信息</b><br>
	 * @param clz
	 * @return	
	 * 
	 */
    private List<JoinFieldMetaData<?>> getJoinMetas(Class<?> clz) {
        Class<?> c = clz;
        List<JoinFieldMetaData<?>> list = new ArrayList<>();
        while (!Object.class.equals(c)) {
            for (Field field : c.getDeclaredFields()) {
                JoinFieldMetaData<?> meta = getJoinMetaByField(clz, field);
                if (null != meta){
                    list.add(meta);
                }
            }
            c = c.getSuperclass();
        }
        return list;
    }

    @SuppressWarnings({"unchecked", "rawtypes" })
    private JoinFieldMetaData<?> getJoinMetaByField(Class<?> clz, Field field) {
        Annotation ann = field.getAnnotation(ManyToMany.class);
        if (null != ann) {
            ParameterizedType pt = (ParameterizedType) field.getGenericType();
            return new JoinFieldMetaData(field, (Class<?>) pt.getActualTypeArguments()[0], clz, 
                    ManyToManyProxy.proxy((ManyToMany) ann));
        }
        ann = field.getAnnotation(OneToMany.class);
        if (null != ann) {
            ParameterizedType pt = (ParameterizedType) field.getGenericType();
            return new JoinFieldMetaData(field, (Class<?>) pt.getActualTypeArguments()[0], clz, 
                    OneToManyProxy.proxy((OneToMany) ann));
        }
        return null;
    }
	
	//通过表名查类信息
	public static Class<?> getClassByTableName(String tableName) {
		return tableMapping.get(tableName);
	}
}
