package rabbit.open.orm.dml;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rabbit.open.orm.annotation.Relation.FilterType;
import rabbit.open.orm.dml.meta.DynamicFilterDescriptor;
import rabbit.open.orm.dml.meta.FieldMetaData;
import rabbit.open.orm.dml.meta.JoinFieldMetaData;
import rabbit.open.orm.dml.meta.MetaData;
import rabbit.open.orm.exception.InvalidJoinFetchOperationException;
import rabbit.open.orm.exception.RepeatedJoinFetchOperationException;

/**
 * <b>Description  关联查询描述符</b>. 
 */
public class FetchDescriptor<T> {

    private AbstractQuery<T> query;
    
    private List<Class<?>> path;

    private Class<?> targetClz = null;
    
    //关联依赖
    private List<Field> dependencyFields = new ArrayList<>();
    
    private Class<?> fetchClz = null;
    
    protected FetchDescriptor(AbstractQuery<T> query){
        path = new ArrayList<>();
        path.add(query.getMetaData().getEntityClz());
        this.query = query;
        targetClz = query.getMetaData().getEntityClz();
    }
    
    /**
     * <b>Description  取出一个多对一的关联对象，不可对同一类型的对象重复调用.</b>
     * @param clz
     * @return
     */
    public FetchDescriptor<T> fetch(Class<?> clz){
        Class<?>[] array = path2Array();
        query.fetch(clz, array);
        path.add(0, clz);
        FieldMetaData fmd = MetaData.getCachedFieldMetaByType(targetClz, clz);
        dependencyFields.add(fmd.getField());
        targetClz = clz;
        return this;
    }
    
    private Class<?>[] path2Array(){
        Class<?>[] array = new Class<?>[path.size()];
        for(int i = 0; i < path.size(); i++){
            array[i] = path.get(i);
        }
        return array;
    }

    private Field[] dep2Array(){
        Field[] array = new Field[dependencyFields.size()];
        for(int i = 0; i < dependencyFields.size(); i++){
            array[i] = dependencyFields.get(i);
        }
        return array;
    }
    
    /**
     * <b>Description  取出一个一对多/多对多的关联对象.</b>
     * @param clz
     * @return
     */
    public FetchDescriptor<T> joinFetch(Class<?> clz){
        fetchClz = clz;
        MetaData<?> meta = MetaData.getMetaByClass(targetClz);
        for(JoinFieldMetaData<?> jfmd : meta.getJoinMetas()){
            if(jfmd.getJoinClass().equals(fetchClz)){
                if(!isRepeatedJoinFetch()){
                    jfmd.setDependencyFields(dep2Array());
                    query.joinFieldMetas.add(jfmd);
                }
                return this;
            }
        }
        throw new InvalidJoinFetchOperationException(fetchClz, targetClz);
    }

    /**
     * <b>Description  检查重复的joinFetch.</b>
     */
    private boolean isRepeatedJoinFetch() {
        for(JoinFieldMetaData<?> jfme : query.joinFieldMetas){
            if(!jfme.getJoinClass().equals(fetchClz)){
                continue;
            }
            if(!jfme.getTargetClass().equals(targetClz)){
                throw new RepeatedJoinFetchOperationException(fetchClz, targetClz, jfme.getTargetClass());
            }else{
                return true;
            }
        }
        return false;
    }
    
    public FetchDescriptor<T> on(String reg, Object value){
        return on(reg, value, FilterType.EQUAL);
    }

    public FetchDescriptor<T> on(String reg, Object value, FilterType filterType){
        String field = query.getFieldByReg(reg);
        query.checkField(fetchClz, field);
        if(!query.addedJoinFilters.containsKey(fetchClz)){
            query.addedJoinFilters.put(fetchClz, new HashMap<String, List<DynamicFilterDescriptor>>());
        }
        if(!query.addedJoinFilters.get(fetchClz).containsKey(field)){
            query.addedJoinFilters.get(fetchClz).put(field, new ArrayList<DynamicFilterDescriptor>());
        }
        query.addedJoinFilters.get(fetchClz).get(field).add(new DynamicFilterDescriptor(reg, filterType, 
                value, !field.equals(reg)));
        return this;
    }
    
    public AbstractQuery<T> build(){
        return this.query;
    }
    
}
