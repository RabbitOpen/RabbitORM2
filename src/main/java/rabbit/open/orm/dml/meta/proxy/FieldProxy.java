package rabbit.open.orm.dml.meta.proxy;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.cglib.proxy.MethodProxy;

import rabbit.open.orm.annotation.Column;
import rabbit.open.orm.annotation.ManyToMany;
import rabbit.open.orm.annotation.OneToMany;
import rabbit.open.orm.annotation.PrimaryKey;

/**
 * <b>Description 字段代理 </b>
 */
public class FieldProxy implements MethodInterceptor {

    // 字段对象本身
    private Field realObject;

    // 字段注解
    private Column column;

    // 主键注解
    private PrimaryKey primaryKey;

    // 多对多注解
    private ManyToMany many2many;

    // 一对多注解
    private OneToMany one2many;

    public void setRealObject(Field field) {
        realObject = field;
        column = ColumnProxy.proxy(realObject.getAnnotation(Column.class));
        primaryKey = PrimaryKeyProxy.proxy(realObject.getAnnotation(PrimaryKey.class));
        many2many = ManyToManyProxy.proxy(realObject.getAnnotation(ManyToMany.class));
        one2many = OneToManyProxy.proxy(realObject.getAnnotation(OneToMany.class));
    }

    /**
     * <b>Description 代理字段对象 </b>
     * @param field
     * @return
     */
    public static Field proxy(Field field) {
        FieldProxy proxy = new FieldProxy();
        proxy.setRealObject(field);
        Enhancer eh = new Enhancer();
        eh.setSuperclass(Field.class);
        eh.setCallback(proxy);
        return (Field) eh.create();
    }

    @Override
    public final Object intercept(Object proxy, Method method, Object[] args,
            MethodProxy methodproxy) throws Throwable {
        if ("getAnnotation".equals(method.getName())) {
            if (Column.class.equals(args[0])) {
                return column;
            }
            if (PrimaryKey.class.equals(args[0])) {
                return primaryKey;
            }
            if (ManyToMany.class.equals(args[0])) {
                return many2many;
            }
            if (OneToMany.class.equals(args[0])) {
                return one2many;
            }
        }
        return method.invoke(realObject, args);
    }
}
