package rabbit.open.orm.core.dml.name;

import rabbit.open.orm.common.exception.RabbitDMLException;

import java.util.ArrayList;
import java.util.List;

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
