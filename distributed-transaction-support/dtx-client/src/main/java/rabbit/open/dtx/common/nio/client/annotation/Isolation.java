package rabbit.open.dtx.common.nio.client.annotation;

/**
 * 事务隔离类型
 * @author xiaoqianbin
 * @date 2019/12/20
 **/
public enum Isolation {

    UNLOCK("该模式下的访问不加锁"),

    LOCK("该模式下的访问会加锁");

    Isolation(String desc) {

    }
}
