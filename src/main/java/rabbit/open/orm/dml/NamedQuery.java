package rabbit.open.orm.dml;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeMap;

import rabbit.open.orm.dml.name.FetcherDescriptor;
import rabbit.open.orm.dml.name.JoinFetcherDescriptor;
import rabbit.open.orm.dml.name.NamedSQL;
import rabbit.open.orm.dml.name.SQLParser;
import rabbit.open.orm.exception.MisMatchedNamedQueryException;
import rabbit.open.orm.pool.SessionFactory;

/**
 * <b>Description: 	命名查询对象</b><br>
 * <b>@author</b>	肖乾斌
 * @param <T>
 * 
 */
public class NamedQuery<T> {

	private NamedSQL nameObject;
	
	private Query<T> query;
	
	private TreeMap<Integer, Object> fieldsValues;
	
	/**
	 * @param fatory
	 * @param clz
	 * @param name		sql名字
	 */
	public NamedQuery(SessionFactory fatory, Class<T> clz, String name) {
		query = new Query<T>(fatory, clz){
		    @Override
		    protected void createQuerySql() {
		        generateQuerySql();
		    }
		    
		    @Override
		    protected void createCountSql() {
		        generateNameCountSql();
		    }
		};
		if(!SQLParser.getQueryByNameAndClass(name, clz).getClass().equals(NamedSQL.class)){
		    throw new MisMatchedNamedQueryException(name);
		}
		nameObject = (NamedSQL) SQLParser.getQueryByNameAndClass(name, clz);
		fieldsValues = new TreeMap<>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
	}

	protected void generateQuerySql() {
	    setEntityAlias();
	    recursivelyFetchEntities();
	    joinFetchEntities();
	    recursivelyJoinFetchEntities(new ArrayList<>(), nameObject.getFetchDescriptors());
	    query.prepareMany2oneFilters();
	    query.createFieldsSql();
	    query.sql = new StringBuilder(nameObject.replaceFields(query.sql.toString()));
		setPreparedValues();
	}
	
	private void generateNameCountSql(){
        query.sql = new StringBuilder(nameObject.replaceFields("COUNT(1)"));
        setPreparedValues();
	}
	

    private void joinFetchEntities() {
        FetchDescriptor<T> buildFetch = query.buildFetch();
	    for(JoinFetcherDescriptor jfd : nameObject.getJoinFetchDescriptors()){
	        buildFetch.joinFetch(jfd.getEntityClass());
        }
    }

    private void recursivelyJoinFetchEntities(List<Class<?>> deps, List<FetcherDescriptor> fetchDescriptors) {
        for(FetcherDescriptor fd : fetchDescriptors){
	        FetchDescriptor<T> bf = query.buildFetch();
	        for(Class<?> dep : deps){
	            bf.fetch(dep);
	        }
	        bf.fetch(fd.getEntityClass());
	        for(JoinFetcherDescriptor jfd : fd.getJoinFetchDescriptors()){
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
	    fetch(nameObject.getFetchDescriptors(), deps);
    }

    private void fetch(List<FetcherDescriptor> fetchDescriptors, List<Class<?>> deps) {
        for(FetcherDescriptor fd : fetchDescriptors){
	        query.fetch(fd.getEntityClass(), deps.toArray(new Class<?>[deps.size()]));
	        List<Class<?>> subDeps = copyList(deps);
            subDeps.add(0, fd.getEntityClass());
	        fetch(fd.getFetchDescriptors(), subDeps);
	    }
    }

    private <D> List<D> copyList(List<D> list){
        List<D> newList = new ArrayList<>();
        for(D d : list){
            newList.add(d);
        }
        return newList;
    }
    
    /**
     * <b>Description  设置别名</b>
     */
    private void setEntityAlias() {
        query.alias(query.getMetaData().getEntityClz(), nameObject.getAlias());
	    setFetchTableAlias(nameObject.getFetchDescriptors());
	    setJoinFetchTableAlias(nameObject.getJoinFetchDescriptors());
    }

    private void setFetchTableAlias(List<FetcherDescriptor> fetchers) {
        for(FetcherDescriptor fd : fetchers){
	        query.alias(fd.getEntityClass(), fd.getAlias());
	        setJoinFetchTableAlias(fd.getJoinFetchDescriptors());
	        setFetchTableAlias(fd.getFetchDescriptors());
	    }
    }

    private void setJoinFetchTableAlias(List<JoinFetcherDescriptor> joinFetchers) {
        for(JoinFetcherDescriptor jfd : joinFetchers){
	        query.alias(jfd.getEntityClass(), jfd.getAlias());
	    }
    }
	
	public Result<T> execute(){
	    return query.execute();
	}
	
	public long count(){
	    return query.count();
	}

    /**
     * 
     * <b>Description:  把setFieldValue设置的值转换到preparedValues中</b><br>.	
     * 
     */
    private void setPreparedValues() {
        if(fieldsValues.isEmpty()){
            return;
        }
        query.preparedValues.addAll(fieldsValues.values());
    }

	/**
	 * 
	 * <b>Description:    单个设值</b><br>.
	 * @param fieldName
	 * @param value
	 * @return	
	 * 
	 */
	public NamedQuery<T> set(String fieldName, Object value){
	    int index = nameObject.getFieldIndex(fieldName);
	    fieldsValues.put(index, value);
	    return this;
	}
	
}
