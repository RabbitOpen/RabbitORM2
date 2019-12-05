package rabbit.open.dtx.client.datasource.proxy;

import java.util.Date;

/**
 * 回滚实体表信息
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class RollBackInfoEntity {

    private Long txBranchId;

    private Long txGroupId;

    // 回滚信息字段
    private byte[] rollbackInfo;

    private Date createdDate = new Date();

    private Date modifiedDate = new Date();

    // 数据源名
    private String datasourceName;

    // 应用名
    private String applicationName;

    public void setTxBranchId(Long txBranchId) {
        this.txBranchId = txBranchId;
    }

    public void setTxGroupId(Long txGroupId) {
        this.txGroupId = txGroupId;
    }

    public void setRollbackInfo(byte[] rollbackInfo) {
        this.rollbackInfo = rollbackInfo;
    }

    public void setCreatedDate(Date createdDate) {
        this.createdDate = createdDate;
    }

    public void setModifiedDate(Date modifiedDate) {
        this.modifiedDate = modifiedDate;
    }

    public void setDatasourceName(String datasourceName) {
        this.datasourceName = datasourceName;
    }

    public void setApplicationName(String applicationName) {
        this.applicationName = applicationName;
    }

    public Long getTxBranchId() {
        return txBranchId;
    }

    public Long getTxGroupId() {
        return txGroupId;
    }

    public byte[] getRollbackInfo() {
        return rollbackInfo;
    }

    public Date getCreatedDate() {
        return createdDate;
    }

    public Date getModifiedDate() {
        return modifiedDate;
    }

    public String getDatasourceName() {
        return datasourceName;
    }

    public String getApplicationName() {
        return applicationName;
    }
}
