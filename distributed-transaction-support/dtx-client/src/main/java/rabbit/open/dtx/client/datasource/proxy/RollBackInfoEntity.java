package rabbit.open.dtx.client.datasource.proxy;

import java.util.Date;

/**
 * 回滚实体表信息
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class RollBackInfoEntity {

    private Long txId;

    private Long txGroupId;

    // 回滚信息字段
    private byte[] rollbackInfo;

    private Date createdDate = new Date();

    private Date modifiedDate = new Date();

    // 数据源名
    private String datasourceName;

    public Long getTxId() {
        return txId;
    }

    public void setTxId(Long txId) {
        this.txId = txId;
    }

    public Long getTxGroupId() {
        return txGroupId;
    }

    public void setTxGroupId(Long txGroupId) {
        this.txGroupId = txGroupId;
    }

    public byte[] getRollbackInfo() {
        return rollbackInfo;
    }

    public void setRollbackInfo(byte[] rollbackInfo) {
        this.rollbackInfo = rollbackInfo;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }
}
