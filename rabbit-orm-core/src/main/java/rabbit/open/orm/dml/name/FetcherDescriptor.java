package rabbit.open.orm.dml.name;

import java.util.ArrayList;
import java.util.List;

import rabbit.open.common.exception.RabbitDMLException;

/**
 * <b>Description  命名查询的fetch行为描述对象</b>
 */
public class FetcherDescriptor {

    private List<FetcherDescriptor> fetchDescriptors = new ArrayList<>();

    private List<JoinFetcherDescriptor> joinFetchDescriptors = new ArrayList<>();
    
    private Class<?> entityClass;
    
    private String alias;

    public FetcherDescriptor(String entityClass,
            String alias) {
        try {
            this.entityClass = Class.forName(entityClass);
        } catch (ClassNotFoundException e) {
            throw new RabbitDMLException(e);
        }
        this.alias = alias;
    }
    
    public List<FetcherDescriptor> getFetchDescriptors() {
        return fetchDescriptors;
    }

    public List<JoinFetcherDescriptor> getJoinFetchDescriptors() {
        return joinFetchDescriptors;
    }

    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getAlias() {
        return alias;
    }

    public void setFetchDescriptors(List<FetcherDescriptor> fetchers) {
        this.fetchDescriptors = fetchers;
    }

    public void setJoinFetchDescriptors(List<JoinFetcherDescriptor> joinFetchers) {
        this.joinFetchDescriptors = joinFetchers;
    }
    
}
