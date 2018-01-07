package rabbit.open.orm.dml.name;

import rabbit.open.orm.exception.RabbitDMLException;

/**
 * <b>Description  命名查询的关联查询描述对象</b>
 */
public class JoinFetcherDescriptor {

    private Class<?> entityClass;
    
    private String alias;
    
    private String joinTableAlias;
    
    public Class<?> getEntityClass() {
        return entityClass;
    }

    public String getAlias() {
        return alias;
    }

    public String getJoinTableAlias() {
        return joinTableAlias;
    }

    /**
     * @param entityClass
     * @param alias
     * @param joinTableAlias
     */
    public JoinFetcherDescriptor(String entityClass, String alias,
            String joinTableAlias) {
        super();
        try {
            this.entityClass = Class.forName(entityClass);
        } catch (ClassNotFoundException e) {
            throw new RabbitDMLException(e);
        }
        this.alias = alias;
        this.joinTableAlias = joinTableAlias;
    }

}
