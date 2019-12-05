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
@Entity("roll_back_info")
public class RollbackEntity {

    @PrimaryKey(policy = Policy.AUTOINCREMENT)
    @Column(value = "id", comment = "主键")
    private Long id;

    @Column(value = "tx_branch_id", comment = "事务分支id")
    private Long txBranchId;

    @Column(value = "tx_group_id", comment = "事务组id")
    private Long txGroupId;

    @Column(value = "rollback_info", comment = "回滚信息")
    private byte[] rollbackInfo;

    @Column(value = "rollback_status", length = 16, comment = "回滚状态")
    private String rollbackStatus;

    @Column(value = "created_date", comment = "创建时间")
    private Date createdDate = new Date();

    @Column(value = "modified_date", comment = "创建时间")
    private Date modifiedDate = new Date();

    @Column(value = "datasource_name", length = 50, comment = "数据源名字")
    private String datasourceName;

    @Column(value = "application_name", length = 50, comment = "应用名字")
    private String applicationName;

    public Long getId() {
        return id;
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
