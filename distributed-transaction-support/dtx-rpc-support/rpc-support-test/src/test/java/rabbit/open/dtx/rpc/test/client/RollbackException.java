package rabbit.open.dtx.rpc.test.client;

/**
 * @author xiaoqianbin
 * @date 2020/1/16
 **/
@SuppressWarnings("serial")
public class RollbackException extends RuntimeException {

    private Long id;

    public RollbackException(long id) {
        super(id + "");
        this.id = id;
    }

    public Long getId() {
        return id;
    }
}
