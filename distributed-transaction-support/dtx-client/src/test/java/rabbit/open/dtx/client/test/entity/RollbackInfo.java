package rabbit.open.dtx.client.test.entity;

import rabbit.open.orm.common.dml.Policy;
import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

import java.util.Date;

/**
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
@Entity("T_ROLL_BACK_INFO")
public class RollbackInfo {

    @PrimaryKey(policy = Policy.AUTOINCREMENT)
    @Column(value = "TX_ID", comment = "事务ID")
    private Long txId;

    @Column(value = "GROUP_ID", comment = "事务组ID")
    private Long groupId;

    @Column(value = "ROLL_BACK_INFO", length = 5000, comment = "回滚信息")
    private String rollbackInfo;

    @Column(value = "CREATED_TIME", comment = "创建日期")
    private Date createdTime;

    public Long getTxId() {
        return txId;
    }

    public void setTxId(Long txId) {
        this.txId = txId;
    }

    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    public String getRollbackInfo() {
        return rollbackInfo;
    }

    public void setRollbackInfo(String rollbackInfo) {
        this.rollbackInfo = rollbackInfo;
    }

    public Date getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(Date createdTime) {
        this.createdTime = createdTime;
    }
}
