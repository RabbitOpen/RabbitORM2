package rabbit.open.orm.core.dml.meta;

import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.common.exception.RepeatedEntityMapping;
import rabbit.open.orm.common.exception.UnKnownFieldException;
import rabbit.open.orm.core.annotation.*;
import rabbit.open.orm.core.dml.DMLObject;
import rabbit.open.orm.core.dml.meta.proxy.DefaultColumnAnnotationGenerator;
import rabbit.open.orm.core.dml.meta.proxy.GenericAnnotationProxy;
import rabbit.open.orm.core.dml.policy.PagePolicy;
import rabbit.open.orm.core.dml.shard.DefaultShardingPolicy;
import rabbit.open.orm.core.dml.shard.ShardingPolicy;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


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
	
	// 分片策略缓存
	private static Map<Class<? extends ShardingPolicy>, ShardingPolicy> policyMapping = new ConcurrentHashMap<>();
	
	//表名
	protected String tableName;
	
	//主键字段Column信息
	protected Column primaryKey;
		
	//实体对应的类的class信息
	protected Class<T> entityClz;
	
	// 分页策略
	private PagePolicy pagePolicy;

	// 默认排序字段
	private String indexOrderedField;

	// 缓存实体类的clz
	private static Map<Class<?>, Boolean> entityClassCache = new ConcurrentHashMap<>();
	
    private MetaData(Class<T> entityClz) {
        this.entityClz = entityClz;
    }

    @SuppressWarnings("rawtypes")
	public static <D> MetaData getMetaByClass(Class<D> clz) {
        if (metaMapping.containsKey(clz)) {
            return metaMapping.get(clz).copyIfSharding(clz);
        }
        cacheMeta(clz);
        return metaMapping.get(clz).copyIfSharding(clz);
    }
    
	/***
	 * <b>@description 如果是分区表则每次都复制一份meta，防止被updateTableName污染表名 </b>
	 * @param <D>
	 * @param clz
	 */
	@SuppressWarnings("unchecked")
	private <D> MetaData<D> copyIfSharding(Class<D> clz) {
		if (!isShardingTable()) {
			return (MetaData<D>) this;
		}
		MetaData<D> meta = new MetaData<>(clz);
		Field[] fields = getClass().getDeclaredFields();
		for (Field f : fields) {
			if (Modifier.isStatic(f.getModifiers())) {
				continue;
			}
			f.setAccessible(true);
			try {
				f.set(meta, f.get(this));
			} catch (Exception e) {
				throw new RabbitDMLException(e);
			}
		}
		return meta;
	}
    
    /**
     * 
     * <b>@description 判断当前表是否是分区表 </b>
     * @return
     */
    public boolean isShardingTable() {
    	return !shardingPolicy.getClass().equals(DefaultShardingPolicy.class);
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
	
	/**
     * 
     * <b>Description:  注册类和表的映射关系</b><br>
     * @param entityClz 
     * 
     */
    private void registClassTableMapping(Class<T> entityClz) {
        Entity entity = entityClz.getAnnotation(Entity.class);
        pagePolicy = entity.pagePolicy();
		tableName = entity.value();
        shardingPolicy = loadShardingPolicy(entityClz);
        primaryKey = getPrimaryKeyFieldMeta(entityClz).getColumn();
        indexOrderedField = loadIndexOrderedField(entityClz, entity);
        if (!tableMapping.containsKey(tableName)) {
        	tableMapping.put(tableName, entityClz);
        } else {
        	if (!entityClz.equals(tableMapping.get(tableName))) {
        		throw new RepeatedEntityMapping(entityClz, tableMapping.get(tableName), tableName);
        	}
        }
        clzMapping.put(entityClz, tableName);
        joinMetas = getJoinMetas(entityClz);
        fieldMetas = getMappingFieldMetas(entityClz).values();
    }

	private String loadIndexOrderedField(Class<T> entityClz, Entity entity) {
        if ("".equals(entity.orderIndexFieldName().trim())) {
        	return getPrimaryKeyFieldMeta(entityClz).getField().getName();
        }
        return entity.orderIndexFieldName().trim();
	}
    
    public PagePolicy getPagePolicy() {
		return pagePolicy;
	}
    
    public String getIndexOrderedField() {
		return indexOrderedField;
	}

    private ShardingPolicy loadShardingPolicy(Class<T> entityClz) {
        Entity entity = entityClz.getAnnotation(Entity.class);
        if (policyMapping.containsKey(entity.shardingPolicy())) {
            return policyMapping.get(entity.shardingPolicy());
        }
        try {
        	if (DefaultShardingPolicy.class.equals(entity.shardingPolicy())) {
        		policyMapping.put(entity.shardingPolicy(), new DefaultShardingPolicy());	
        	} else {
        		policyMapping.put(entity.shardingPolicy(), DMLObject.newInstance(entity.shardingPolicy()));
        	}
        } catch (Exception e) {
            throw new RabbitDMLException(e);
        }
        return policyMapping.get(entity.shardingPolicy());
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
        boolean autoSpeculate = clzz.getAnnotation(Entity.class).autoSpeculate();
        while (!clz.equals(Object.class)) {
            lookupMappingFieldMetasByClass(clz, fields, counter, autoSpeculate);
            clz = clz.getSuperclass();
        }
        return fields;
    }

    private static void lookupMappingFieldMetasByClass(Class<?> clz, Map<String, FieldMetaData> fields,
													   Map<Class<?>, Integer> counter, boolean autoSpeculate) {
        for (Field f : clz.getDeclaredFields()) {
            Column col = f.getAnnotation(Column.class);
            if (null == col && autoSpeculate) {
                col = createDefaultColumnInfo(f);
            }
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
                fmd.setMultiFetchField(true);
                flagMultiFetchFieldByType(fields, f.getType());
            }
            fields.put(f.getName(), fmd);
        }
    }

    /**
     * 生成默认注解
     * @param	f
     * @author  xiaoqianbin
     * @date    2020/4/20
     **/
    private static Column createDefaultColumnInfo(Field f) {
        if (isPrimaryType(f.getType())) {
            return DefaultColumnAnnotationGenerator.proxy(f.getName());
        }
        return null;
    }

    /**
     * 判断type是否是基础数据类型
     * @param	type
     * @author  xiaoqianbin
     * @date    2020/4/20
     **/
    private static boolean isPrimaryType(Class<?> type) {
        Class<?>[] classes = new Class<?>[]{
                String.class, Date.class, Integer.class, Short.class,
                Long.class, Short.class, Double.class, Float.class, Boolean.class};
        for (Class<?> clz : classes) {
            if (type.equals(clz)) {
                return true;
            }
        }
        return type.isEnum();
    }

    //标识type对应的字段为mutiFetchField
    private static void flagMultiFetchFieldByType(Map<String, FieldMetaData> fields,
												  Class<?> type) {
        for (FieldMetaData fmd : fields.values()) {
            if (fmd.getField().getType().equals(type)) {
                fmd.setMultiFetchField(true);
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
	
	public static Field getPrimaryKeyField(Class<?> objClz) {
		Class<?> clz = objClz;
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
		if (entityClassCache.containsKey(clz)) {
			return true;
		}
		if (null != clz.getAnnotation(Entity.class)) {
			entityClassCache.put(clz, Boolean.TRUE);
			return true;
		}
		return false;
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

	public static void setFieldsAliasMapping(Class<?> clz, Map<String, String> mapping) {
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
                if (null != meta) {
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
            		GenericAnnotationProxy.proxy((ManyToMany) ann, ManyToMany.class));
        }
        ann = field.getAnnotation(OneToMany.class);
        if (null != ann) {
            ParameterizedType pt = (ParameterizedType) field.getGenericType();
            return new JoinFieldMetaData(field, (Class<?>) pt.getActualTypeArguments()[0], clz, 
            		GenericAnnotationProxy.proxy((OneToMany) ann, OneToMany.class));
        }
        return null;
    }
	
	//通过表名查类信息
	public static Class<?> getClassByTableName(String tableName) {
		return tableMapping.get(tableName);
	}
}
