package rabbit.open.dtx.client.test.entity;

import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.annotation.Entity;
import rabbit.open.orm.core.annotation.PrimaryKey;

import java.util.Date;

/**
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
@Entity("roll_back_info")
public class RollbackEntity {

    @PrimaryKey
    @Column("tx_id")
    private Long txId;

    @Column("tx_group_id")
    private Long txGroupId;

    @Column("rollback_info")
    private byte[] rollbackInfo;

    @Column(value = "rollback_status", length = 16)
    private String rollbackStatus;

    @Column(value = "created_date", comment = "创建时间")
    private Date createdDate = new Date();

    @Column(value = "modified_date", comment = "创建时间")
    private Date modifiedDate = new Date();

    // 数据源名
    @Column(value = "datasource_name", length = 50)
    private String datasourceName;

    public Long getTxId() {
        return txId;
    }

    public Long getTxGroupId() {
        return txGroupId;
    }

    public byte[] getRollbackInfo() {
        return rollbackInfo;
    }

    public String getRollbackStatus() {
        return rollbackStatus;
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
}
