package rabbit.open.orm.dml.meta;

import java.util.List;

import rabbit.open.orm.dml.JoinFilterDescriptor;


/**
 * <b>Description:   内链接过滤条件</b>.
 * <b>@author</b>    肖乾斌
 * 
 */
public class JoinFilter {

    //过滤条件描述对象
    private JoinFilterDescriptor descriptor;
    
    protected JoinFilter(){
        
    }

    public void setDescriptor(JoinFilterDescriptor descriptor) {
        this.descriptor = descriptor;
    }
    
    /**
     * 
     * <b>Description:  获取内链接字段描述元数据</b><br>.
     * @return	
     * 
     */
    public JoinFieldMetaData<?> getJoinFieldMetaData(){
        return descriptor.getJoinFieldMetaData();
    }
    
    /**
     * 
     * <b>Description:  获取内链接sql</b><br>.
     * @return	
     * 
     */
    public StringBuilder getInnerJoinSQL(){
        return descriptor.getInnerJoinSQL();
    }
    
    /**
     * 
     * <b>Description:  获取多端实体的class</b><br>.
     * @return	
     * 
     */
    public Class<?> getJoinClass(){
        return descriptor.getClz();
    }
    
    /**
     * 
     * <b>Description:  获取相关联的class</b><br>.
     * @return  
     * 
     */
    public List<Class<?>> getAssocicatedClass(){
        return descriptor.getAssocicatedClass();
    }
}
