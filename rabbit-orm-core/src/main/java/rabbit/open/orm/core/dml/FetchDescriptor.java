package rabbit.open.orm.core.dml;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import rabbit.open.orm.common.dml.FilterType;
import rabbit.open.orm.common.exception.InvalidJoinFetchOperationException;
import rabbit.open.orm.common.exception.RepeatedJoinFetchOperationException;
import rabbit.open.orm.core.dml.meta.DynamicFilterDescriptor;
import rabbit.open.orm.core.dml.meta.FieldMetaData;
import rabbit.open.orm.core.dml.meta.JoinFieldMetaData;
import rabbit.open.orm.core.dml.meta.MetaData;

/**
 * <b>Description  关联查询描述符</b>. 
 */
public class FetchDescriptor<T> {

    private AbstractQuery<T> query;
    
    private List<Class<?>> path;

    private Class<?> targetClz = null;
    
    //关联依赖
    private List<Field> dependencyFields = new ArrayList<>();
    
    private Class<?> joinFetchClz = null;

    protected FetchDescriptor(AbstractQuery<T> query) {
        path = new ArrayList<>();
        path.add(query.getMetaData().getEntityClz());
        this.query = query;
        targetClz = query.getMetaData().getEntityClz();
    }
    
    /**
     * <b>Description  取出一个多对一的关联对象</b>
     * @param clz
     * @return
     */
    public FetchDescriptor<T> fetch(Class<?> clz) {
        Class<?>[] array = path2Array();
        query.fetch(clz, array);
        path.add(0, clz);
        FieldMetaData fmd = MetaData.getCachedFieldMetaByType(targetClz, clz);
        dependencyFields.add(fmd.getField());
        targetClz = clz;
        return this;
    }

    private Class<?>[] path2Array() {
        Class<?>[] array = new Class<?>[path.size()];
        for (int i = 0; i < path.size(); i++) {
            array[i] = path.get(i);
        }
        return array;
    }

    private Field[] dep2Array() {
        Field[] array = new Field[dependencyFields.size()];
        for (int i = 0; i < dependencyFields.size(); i++) {
            array[i] = dependencyFields.get(i);
        }
        return array;
    }
    
    /**
     * <b>Description  取出一个一对多/多对多的关联对象.</b>
     * @param clz
     * @return
     */
    public JoinFetcher<T> joinFetch(Class<?> clz) {
        query.checkShardedFetch(clz);
        joinFetchClz = clz;
        MetaData<?> meta = MetaData.getMetaByClass(targetClz);
        for (JoinFieldMetaData<?> jfmd : meta.getJoinMetas()) {
            if (jfmd.getJoinClass().equals(joinFetchClz)) {
                if (!isRepeatedJoinFetch()) {
                    jfmd.setDependencyFields(dep2Array());
                    query.joinFieldMetas.add(jfmd.clone());
                }
                return new JoinFetcher<>(this);
            }
        }
        throw new InvalidJoinFetchOperationException(joinFetchClz, targetClz);
    }

    /**
     * <b>Description  检查重复的joinFetch.</b>
     */
    private boolean isRepeatedJoinFetch() {
        for (JoinFieldMetaData<?> jfme : query.joinFieldMetas) {
            if (!jfme.getJoinClass().equals(joinFetchClz)) {
                continue;
            }
            if (!jfme.getTargetClass().equals(targetClz)) {
                throw new RepeatedJoinFetchOperationException(joinFetchClz, targetClz, jfme.getTargetClass());
            } else {
                return true;
            }
        }
        return false;
    }

    protected FetchDescriptor<T> on(String reg, Object value, FilterType filterType) {
        String field = query.getFieldByReg(reg);
        DMLObject.checkField(joinFetchClz, field);
        if (!query.addedJoinFilters.containsKey(joinFetchClz)) {
            query.addedJoinFilters.put(joinFetchClz, new HashMap<String, List<DynamicFilterDescriptor>>());
        }
        if (!query.addedJoinFilters.get(joinFetchClz).containsKey(field)) {
            query.addedJoinFilters.get(joinFetchClz).put(field, new ArrayList<DynamicFilterDescriptor>());
        }
        query.addedJoinFilters.get(joinFetchClz).get(field).add(new DynamicFilterDescriptor(reg, filterType,
                value, !field.equals(reg)));
        return this;
    }

    public AbstractQuery<T> build() {
        return this.query;
    }
    
}
