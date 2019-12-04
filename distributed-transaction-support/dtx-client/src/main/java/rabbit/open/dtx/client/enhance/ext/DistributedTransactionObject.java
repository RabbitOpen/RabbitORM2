package rabbit.open.dtx.client.enhance.ext;

/**
 * 事务对象
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class DistributedTransactionObject {

    // 事务id
    private Long txId;

    public DistributedTransactionObject(Long txId) {
        setTxId(txId);
    }

    public Long getTxId() {
        return txId;
    }

    public void setTxId(Long txId) {
        this.txId = txId;
    }
}
