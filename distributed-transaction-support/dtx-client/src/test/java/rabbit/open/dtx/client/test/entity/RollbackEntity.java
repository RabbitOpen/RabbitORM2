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
    @Column(value = "ID", comment = "主键")
    private Long id;

    @Column(value = "TX_BRANCH_ID", comment = "事务分支id")
    private Long txBranchId;

    @Column(value = "TX_GROUP_ID", comment = "事务组id")
    private Long txGroupId;

    @Column(value = "ROLLBACK_INFO", comment = "回滚信息")
    private byte[] rollbackInfo;

    @Column(value = "ROLLBACK_STATUS", length = 16, comment = "回滚状态")
    private String rollbackStatus;

    @Column(value = "CREATED_DATE", comment = "创建时间")
    private Date createdDate = new Date();

    @Column(value = "MODIFIED_DATE", comment = "创建时间")
    private Date modifiedDate = new Date();

    @Column(value = "DATASOURCE_NAME", length = 50, comment = "数据源名字")
    private String datasourceName;

    @Column(value = "APPLICATION_NAME", length = 50, comment = "应用名字")
    private String applicationName;

    @Column(value = "ROLLBACK_ORDER", comment = "回滚顺序")
    private Long rollbackOrder;

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
