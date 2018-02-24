package oracle.test.filter;

import java.lang.reflect.Field;
import java.util.List;

import oracle.test.entity.UUIDPolicyEntity;
import oracle.test.entity.User;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import rabbit.open.orm.dml.filter.DMLFilter;
import rabbit.open.orm.dml.filter.DMLType;

@Component
public class MyDMLFilter implements DMLFilter {

    Logger logger = Logger.getLogger(getClass());
    
    @Override
    public Object onValueSetted(Object value, Field field, DMLType type) {
        logger.info(field.getDeclaringClass().getSimpleName() + "." + field.getName() + ":\t" + value + ":\t" + type);
        if (UUIDPolicyEntity.class.equals(field.getDeclaringClass())) {
            try {
                if (field.equals(UUIDPolicyEntity.class.getDeclaredField("id"))) {
                    return "123";
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return value;
    }

    @Override
    public List<Object> queryCompleted(List<Object> result, Class<?> clz) {
        if (User.class.equals(clz)) {
           for (Object o : result) {
               User u = (User) o;
               u.setAge(1000);
           }
        }
        return result;
    }
}
