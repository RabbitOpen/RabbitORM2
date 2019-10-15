package oracle.test.interceptor;

import java.lang.reflect.Field;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import oracle.test.entity.UUIDPolicyEntity;
import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.core.dml.interceptor.DMLInterceptor;

@Component
public class MyDMLInterceptor implements DMLInterceptor {

    Logger logger = LoggerFactory.getLogger(getClass());
    
    @Override
    public Object onValueSet(Object value, Field field, DMLType type) {
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
    public Object onValueGot(Object value, Field field) {
        return value;
    }

   
}
