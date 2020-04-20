package rabbit.open.orm.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * <b>@description 声明sql映射接口对应的实体类 </b>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.TYPE)
public @interface NameSpace {

    /**
     * <b>@description 该mapper对应的实体class </b>
     * @return
     */
    Class<?> value();
}
