package rabbit.open.orm.dml.meta;

import rabbit.open.common.annotation.FilterType;
import rabbit.open.orm.dml.AbstractQuery;
import rabbit.open.orm.dml.JoinFilterDescriptor;

public class JoinFilterBuilder {

    private JoinFilterDescriptor filterDesc;
    
    //最上层的关联查询对象
    private JoinFilter filter;
    
    private AbstractQuery<?> query;
    
    private JoinFilterBuilder(AbstractQuery<?> query) {
        super();
        this.query = query;
    }

    /**
     * 
     * <b>Description:  新建一个JoinFilterBuilder</b><br>.
     * @param query
     * @return	
     * 
     */
    public static JoinFilterBuilder prepare(AbstractQuery<?> query){
        return new JoinFilterBuilder(query);
    }
    
    /**
     * 
     * <b>Description:  在现有的关联查询对象的基础上关联一个一对多或者多对多的clz</b><br>.
     * @param joinClz
     * @return	
     * 
     */
    public JoinFilterBuilder join(Class<?> joinClz){
        if(null == filterDesc){
            filterDesc = new JoinFilterDescriptor(joinClz, query);
            filter = new JoinFilter();
            filter.setDescriptor(filterDesc);
        }else{
            JoinFilterDescriptor subDecriptor = new JoinFilterDescriptor(joinClz, filterDesc.getTargetClzQuery());
            filterDesc.setSubJoinFilterDescriptor(subDecriptor);
            filterDesc = subDecriptor;
        }
        return this;
    }
    
    /**
     * 
     * <b>Description:  新增过滤条件</b><br>.
     * @param fieldReg  字段     
     * @param value     字段的值
     * @param ft        过滤条件
     * @return  
     * 
     */
    public JoinFilterBuilder on(String fieldReg, Object value, FilterType ft){
        filterDesc.on(fieldReg, value, ft);
        return this;
    }

    /**
     * 
     * <b>Description:  新增过滤条件</b><br>.
     * @param fieldReg  字段     
     * @param value     字段的值
     * @return	
     * 
     */
    public JoinFilterBuilder on(String fieldReg, Object value){
        on(fieldReg, value, FilterType.EQUAL);
        return this;
    }
    
    /**
     * 
     * <b>Description:  返回构建的JoinFilter</b><br>.
     * @return	
     * 
     */
    public JoinFilter build(){
        return filter;
    }
    
}
