package oracle.test.filter;

import java.lang.reflect.Field;

import oracle.test.entity.UUIDPolicyEntity;

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
    public Object onValueGetted(Object value, Field field) {
        return value;
    }

   
}
