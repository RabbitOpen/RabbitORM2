package rabbit.open.orm.core.dml.name;

import rabbit.open.orm.common.exception.RabbitDMLException;

/**
 * <b>Description  命名查询的关联查询描述对象</b>
 */
public class JoinFetcherDescriptor {

    private Class<?> entityClass;
    
    private String alias;
    
    public JoinFetcherDescriptor(String entityClass, String alias) {
        try {
            this.entityClass = Class.forName(entityClass);
        } catch (ClassNotFoundException e) {
            throw new RabbitDMLException(e);
        }
        this.alias = alias;
    }
    
    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getAlias() {
        return alias;
    }

}
