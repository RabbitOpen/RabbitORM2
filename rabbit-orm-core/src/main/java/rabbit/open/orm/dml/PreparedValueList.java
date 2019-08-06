package rabbit.open.orm.dml;

import java.util.ArrayList;

@SuppressWarnings("serial")
public class PreparedValueList<T> extends ArrayList<T> {

    private int index = 0;
    
    @Override
    public void clear() {
        super.clear();
        index = 0;
    }
    
    public T get() {
        T t = super.get(index);
        index++;
        return t;
    }
    
}
