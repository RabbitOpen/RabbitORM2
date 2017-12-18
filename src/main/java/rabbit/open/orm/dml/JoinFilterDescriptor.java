package rabbit.open.orm.dml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rabbit.open.orm.annotation.ManyToMany;
import rabbit.open.orm.annotation.Relation.FilterType;
import rabbit.open.orm.dml.meta.DynamicFilterDescriptor;
import rabbit.open.orm.dml.meta.JoinFieldMetaData;
import rabbit.open.orm.exception.RabbitDMLException;


/**
 * <b>Description:   一对多/多对多内链接过滤条件</b>.
 * <b>@author</b>    肖乾斌
 * 
 */
public class JoinFilterDescriptor {

    //多端实体类的class
    protected Class<?> clz;

    //下级过滤条件描述符
    private JoinFilterDescriptor subFilterDescriptor;
    
    protected AbstractQuery<?> query;
    
    private AbstractQuery<?> targetClzQuery;
    
    private JoinFieldMetaData<?> joinFieldMetaData;
    
    //动态添加的inner join过滤器
    private Map<Class<?>, Map<String, List<DynamicFilterDescriptor>>> joinFilters;
    
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public JoinFilterDescriptor(Class<?> joinClz, AbstractQuery<?> query) {
        super();
        this.clz = joinClz;
        this.query = query;
        joinFilters = new HashMap<>();
        findJoinFieldMetaData();
        targetClzQuery = new Query(query.sessionFactory, joinClz){
            
            /**
             * 重载获取别名方法，调用上级query的别名管理方法实现统一别名管理
             */
            @Override
            public String getAliasByTableName(String tableName) {
                return JoinFilterDescriptor.this.query.getAliasByTableName(tableName);
            }
            
            /** 
             * 重载cachePreparedValues方法，将jdbc存储过程的值存储到顶级query中
             */
            @Override
            protected void cachePreparedValues(Object value) {
                JoinFilterDescriptor.this.query.cachePreparedValues(value);
            }
        };
    }
    
    /**
     * 
     * <b>Description:  获取相关联的class</b><br>.
     * @return	
     * 
     */
    public List<Class<?>> getAssocicatedClass(){
        List<Class<?>> clzes = new ArrayList<>();
        clzes.add(getClz());
        if(null != this.subFilterDescriptor){
            clzes.addAll(subFilterDescriptor.getAssocicatedClass());
        }
        return clzes;
    }
    
    public AbstractQuery<?> getTargetClzQuery() {
        return targetClzQuery;
    }
    
    /**
     * 
     * <b>Description:  新增过滤条件</b><br>.
     * @param fieldReg  字段     
     * @param value     字段的值
     * @param ft        过滤条件类型
     * @return	
     * 
     */
    public JoinFilterDescriptor on(String fieldReg, Object value, FilterType ft){
        String field = query.getFieldByReg(fieldReg);
        query.checkField(clz, field);
        if(!joinFilters.containsKey(clz)){
            joinFilters.put(clz, new HashMap<String, List<DynamicFilterDescriptor>>());
        }
        if(!joinFilters.get(clz).containsKey(field)){
            joinFilters.get(clz).put(field, new ArrayList<DynamicFilterDescriptor>());
        }
        joinFilters.get(clz).get(field).add(new DynamicFilterDescriptor(fieldReg, ft, value, !field.equals(fieldReg)));
        return this;
    }
    
    public JoinFilterDescriptor on(String fieldReg, Object value){
        return on(fieldReg, value, FilterType.EQUAL);
    }
    
    public void setSubJoinFilterDescriptor(JoinFilterDescriptor filter) {
        this.subFilterDescriptor = filter;
    }
    
    /**
     * 
     * <b>Description:  获取内链接关联查询sql</b><br>.
     * @return	
     * 
     */
    public StringBuilder getInnerJoinSQL(){
        StringBuilder sql;
        if(joinFieldMetaData.getAnnotation() instanceof ManyToMany){
            sql = query.createMTMJoinSql(joinFieldMetaData, false);
            sql.append(query.addDynFilterSql(joinFieldMetaData, joinFilters));
        }else{
            sql = query.createOTMJoinSql(joinFieldMetaData, false);
            sql.append(query.addDynFilterSql(joinFieldMetaData, joinFilters));
        }
        if(null != subFilterDescriptor){
            sql.append(subFilterDescriptor.getInnerJoinSQL());
        }
        return sql;
    }

    /**
     * 
     * <b>Description:  查找正确的joinFieldMetaData</b><br>.	
     * 
     */
    private void findJoinFieldMetaData() {
        List<JoinFieldMetaData<?>> joinMetas = this.query.getMetaData().getJoinMetas();
        for(JoinFieldMetaData<?> jfm : joinMetas){
            if(clz.equals(jfm.getJoinClass())){
                joinFieldMetaData = jfm;
                return;
            }
        }
        throw new RabbitDMLException("[" + clz.getName() + "] can't be joined by [" 
                + query.getMetaData().getEntityClz().getName() + "]");
    }
    
    public JoinFieldMetaData<?> getJoinFieldMetaData() {
        return joinFieldMetaData;
    }
    
    public Class<?> getClz() {
        return clz;
    }
}
