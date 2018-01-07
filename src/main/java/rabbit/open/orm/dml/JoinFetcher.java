package rabbit.open.orm.dml;

import rabbit.open.orm.annotation.Relation.FilterType;

public class JoinFetcher<T> {

    private FetchDescriptor<T> descriptor;

    public JoinFetcher(FetchDescriptor<T> descriptor) {
        super();
        this.descriptor = descriptor;
    }
    
    public AbstractQuery<T> build(){
        return this.descriptor.build();
    }
    
    public FetchDescriptor<T> fetch(Class<?> clz){
        return this.descriptor.fetch(clz);
    }
    
    public FetchDescriptor<T> on(String reg, Object value){
        return on(reg, value, FilterType.EQUAL);
    }
    
    public FetchDescriptor<T> on(String reg, Object value, FilterType filterType){
        return this.descriptor.on(reg, value, filterType);
    }
}
