package rabbit.open.dtx.client.enhance;

/**
 * 事务隔离级别
 * @author xiaoqianbin
 * @date 2019/12/20
 **/
public enum Isolation {

    READ_UNCOMMITTED("读未提交，该模式下的访问不加锁"),

    READ_COMMITTED("读已提交，该模式下的访问会加锁");

    Isolation(String desc) {

    }
}
