package rabbit.open.orm.dml.meta;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import rabbit.open.orm.annotation.Column;
import rabbit.open.orm.annotation.Entity;
import rabbit.open.orm.annotation.ManyToMany;
import rabbit.open.orm.annotation.OneToMany;
import rabbit.open.orm.annotation.PrimaryKey;
import rabbit.open.orm.exception.RabbitDMLException;
import rabbit.open.orm.exception.UnKnownFieldException;
import rabbit.open.orm.shard.ShardingPolicy;


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
	private static ConcurrentHashMap<Class<?>, List<FieldMetaData>> fieldsMapping = new ConcurrentHashMap<>();
	
	//主键缓存
	private static Map<Class<?>, Field> primaryKeyMapping = new ConcurrentHashMap<>();
	
	//oracle数据库不区分大小写，所以结果集字段和实体类字段需要映射一下
	private static Map<Class<?>, Map<String, String>> fieldsAlias = new ConcurrentHashMap<>();
	
	//字段元信息
	private List<FieldMetaData> fieldMetas;
	
	//能进行一对多、多对多查询的实体信息
	protected List<JoinFieldMetaData<?>> joinMetas;
	
	private ShardingPolicy shardingPolicy;
	
	private Map<Class<? extends ShardingPolicy>, ShardingPolicy> policyMapping = new ConcurrentHashMap<>();
	
	//表名
	protected String tableName;
	
	//主键字段名
	protected String primaryKey;
		
	//实体对应的类的class信息
	protected Class<T> entityClz;
	
	public MetaData(Class<T> entityClz) {
        this.entityClz = entityClz;
        if (metaMapping.containsKey(entityClz)) {
            return;
        }
        registClassTableMapping(entityClz);
    }
    
    public static <D> MetaData<?> getMetaByClass(Class<D> clz){
        if(metaMapping.containsKey(clz)){
            return metaMapping.get(clz);
        }
        metaMapping.put(clz, new MetaData<D>(clz));
        return metaMapping.get(clz);
    }

	public List<JoinFieldMetaData<?>> getJoinMetas() {
		return joinMetas;
	}

	public String getTableName() {
		return tableName;
	}

	public String getPrimaryKey() {
		return primaryKey;
	}

	public ShardingPolicy getShardingPolicy() {
        return shardingPolicy;
    }
	
	public static String getPrimaryKey(Class<?> clz) {
	    return getPrimaryKeyField(clz).getAnnotation(Column.class).value();
	}

	/**
     * 
     * <b>Description:  注册类和表的映射关系</b><br>
     * @param entityClz 
     * 
     */
    private synchronized void registClassTableMapping(Class<T> entityClz) {
        if (metaMapping.containsKey(entityClz)) {
            return;
        }
        tableName = entityClz.getAnnotation(Entity.class).value();
        shardingPolicy = loadShardingPolicy(entityClz);
        primaryKey = getPrimaryKeyField(entityClz).getAnnotation(Column.class).value();
        tableMapping.put(tableName, entityClz);
        clzMapping.put(entityClz, tableName);
        joinMetas = getJoinMetas(entityClz);
        fieldMetas = getMappingFieldMetas(entityClz);
    }

    private ShardingPolicy loadShardingPolicy(Class<T> entityClz) {
        Entity entity = entityClz.getAnnotation(Entity.class);
        if (policyMapping.containsKey(entity.policy())) {
            return policyMapping.get(entity.policy());
        }
        try {
            policyMapping.put(entity.policy(), entity.policy().newInstance());
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
	private static List<FieldMetaData> getMappingFieldMetas(Class<?> clzz){
	    Class<?> clz = clzz;
		List<FieldMetaData> fields = new ArrayList<>();
		Map<Class<?>, Integer> counter = new HashMap<>();
		while(!clz.equals(Object.class)){
			for(Field f : clz.getDeclaredFields()){
				Column col = f.getAnnotation(Column.class);
				if(null != col){
				    if(counter.containsKey(f.getType())){
				        counter.put(f.getType(), counter.get(f.getType()) + 1);
				    }else{
				        counter.put(f.getType(), 1);
				    }
					FieldMetaData fmd = new FieldMetaData(f, col);
					fmd.setIndex(counter.get(f.getType()));
					if(fmd.getIndex() > 1){
					    fmd.setMutiFetchField(true);
					    flagMutiFetchFieldByType(fields, f.getType());
					}
                    fields.add(fmd);
				}
			}
			clz = clz.getSuperclass();
		}
		return fields;
	}

	//标识type对应的字段为mutiFetchField
    private static void flagMutiFetchFieldByType(List<FieldMetaData> fields, Class<?> type) {
        for(FieldMetaData fmd : fields){
            if(fmd.getField().getType().equals(type)){
                fmd.setMutiFetchField(true);
            }
        }
    }

	public List<FieldMetaData> getFieldMetas() {
		return fieldMetas;
	}
	
	/**
	 * 
	 * 查找主键字段
	 * @param clzz
	 * @return
	 * 
	 */
	public static Field getPrimaryKeyField(Class<?> clzz){
	    Class<?> clz = clzz;
		if(null != primaryKeyMapping.get(clz)){
			return primaryKeyMapping.get(clz);
		}
		String name = clz.getName();
		while(true){
			for(Field f : clz.getDeclaredFields()){
				PrimaryKey pk = f.getAnnotation(PrimaryKey.class);
				if(null != pk ){
					primaryKeyMapping.put(clz, f);
					return f;
				}
			}
			clz = clz.getSuperclass();
			if(clz.equals(Object.class)){
				break;
			}
		}
		throw new RabbitDMLException("no primary key was found in [" + name + "]");
	}
	
	//通过类信息查表名
	public static String getTableNameByClass(Class<?> clz){
		String tableName = clzMapping.get(clz);
		if(null == tableName){
			Class<?> c = clz;
			while(null ==  c.getAnnotation(Entity.class)){
				 c =  c.getSuperclass();
				 if(c == null){
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
	public static boolean isEntityClass(Class<?> clz){
		return null != clz.getAnnotation(Entity.class);
	}
	
	/**
	 * 
	 * 更新表和类的映射
	 * @param table
	 * @param clz
	 * 
	 */
	public static void updateTableMapping(String table, Class<?> clz){
		if(tableMapping.containsKey(table)){
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
	public static List<FieldMetaData> getCachedFieldsMetas(Class<?> clz){
		List<FieldMetaData> mapping = fieldsMapping.get(clz);
		if(null != mapping){
			return mapping;
		}else{
			Class<?> c = clz;
			while(!c.equals(Object.class)){
				c = c.getSuperclass();
				if(null != fieldsMapping.get(c)){
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
	public static FieldMetaData getCachedFieldMetaByType(Class<?> clz, Class<?> type){
	    List<FieldMetaData> cachedFieldsMetas = getCachedFieldsMetas(clz);
	    for(FieldMetaData fmd : cachedFieldsMetas){
	        if(fmd.getField().getType().equals(type)){
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
	public static FieldMetaData getCachedFieldsMeta(Class<?> clz, String fieldName){
		for(FieldMetaData fmd : getCachedFieldsMetas(clz)){
			if(fmd.getField().getName().equals(fieldName)){
				return fmd;
			}
		}
		throw new UnKnownFieldException("field[" + fieldName + "] does not belong to class[" 
                + clz.getName() + "]");
	}
	
	//字段别名映射 key 是别名 value是字段名
	public static Map<String, String> getFieldsAliasMapping(Class<?> clz){
		return fieldsAlias.get(clz);
	}
	
	public static void setFieldsAliasMapping(Class<?> clz, Map<String, String> mapping){
		fieldsAlias.put(clz, mapping);
	}
		
	/**
	 * 
	 * <b>Description:	获取能够被一对多，多对多查询的字段的meta信息</b><br>
	 * @param clz
	 * @return	
	 * 
	 */
	@SuppressWarnings({"unchecked", "rawtypes" })
	private List<JoinFieldMetaData<?>> getJoinMetas(Class<?> clz){
		Class<?> c = clz;
		List<JoinFieldMetaData<?>> jm = new ArrayList<>();
		while(!Object.class.equals(c)){
			for(Field f : c.getDeclaredFields()){
				Annotation ann = f.getAnnotation(ManyToMany.class);
				if(null != ann){
					ParameterizedType pt = (ParameterizedType) f.getGenericType();
					jm.add(new JoinFieldMetaData(f, (Class<?>) pt.getActualTypeArguments()[0], clz, ann));
					continue;
				}
				ann = f.getAnnotation(OneToMany.class);
				if(null != ann){
					ParameterizedType pt = (ParameterizedType) f.getGenericType();
					jm.add(new JoinFieldMetaData(f, (Class<?>) pt.getActualTypeArguments()[0], clz, ann));
					continue;
				}
			}
			c = c.getSuperclass();
		}
		return jm;
	}
	
	//通过表名查类信息
	public static Class<?> getClassByTableName(String tableName){
		return tableMapping.get(tableName);
	}
}
