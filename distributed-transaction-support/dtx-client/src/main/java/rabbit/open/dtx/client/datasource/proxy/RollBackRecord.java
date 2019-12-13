package rabbit.open.dtx.client.datasource.proxy;

import java.util.Date;

/**
 * 回滚记录实体表信息
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class RollBackRecord {

    public static final String PRIMARY_KEY = "ID";

    public static final String TX_BRANCH_ID = "TX_BRANCH_ID";

    public static final String TX_GROUP_ID = "TX_GROUP_ID";

    public static final String ROLLBACK_INFO = "ROLLBACK_INFO";

    public static final String DATASOURCE_NAME = "DATASOURCE_NAME";

    public static final String APPLICATION_NAME = "APPLICATION_NAME";

    public static final String ROLLBACK_STATUS = "ROLLBACK_STATUS";

    public static final String ROLLBACK_ORDER = "ROLLBACK_ORDER";

    public static final String CREATED_DATE = "CREATED_DATE";

    public static final String MODIFIED_DATE = "MODIFIED_DATE";

    private static final String SEPARATOR = ", ";

    private static final String AND = " = ? and ";

    // 插入数据库的sql
    public static final String INSERT_SQL = new StringBuilder("insert into roll_back_info (")
                    .append(TX_BRANCH_ID + SEPARATOR)
                    .append(TX_GROUP_ID + SEPARATOR)
                    .append(ROLLBACK_INFO + SEPARATOR)
                    .append(DATASOURCE_NAME + SEPARATOR)
                    .append(APPLICATION_NAME + SEPARATOR)
                    .append(ROLLBACK_STATUS + SEPARATOR)
                    .append(CREATED_DATE + SEPARATOR)
                    .append(MODIFIED_DATE + SEPARATOR)
                    .append(ROLLBACK_ORDER + ") values (?, ?, ?, ?, ?, ?, ?, ?, ?)").toString();

    // 查询sql
    public static final String QUERY_SQL = new StringBuilder("select ")
                    .append(PRIMARY_KEY + SEPARATOR)
                    .append(TX_BRANCH_ID + SEPARATOR)
                    .append(TX_GROUP_ID + SEPARATOR + ROLLBACK_INFO)
                    .append(" from roll_back_info where ")
                    .append(TX_GROUP_ID + AND)
                    .append(TX_BRANCH_ID + AND)
                    .append(APPLICATION_NAME + AND)
                    .append(DATASOURCE_NAME + " = ? order by " + ROLLBACK_ORDER + " desc ")
                    .toString();
    // 删除sql
    public static final String DELETE_SQL = new StringBuilder("delete from roll_back_info where ")
                    .append(PRIMARY_KEY + " = ?")
                    .toString();

    // 回滚失败接口
    public static final String UPDATE_SQL = new StringBuilder("update roll_back_info set ")
                    .append(ROLLBACK_STATUS + " = 1 where ")
                    .append(PRIMARY_KEY + " = ?")
                    .toString();
    // 主键id
    private Long id;

    // 事务分支id
    private Long txBranchId;

    // 事务组id
    private Long txGroupId;

    // 回滚信息字段
    private byte[] rollbackInfo;

    private Date createdDate = new Date();

    private Date modifiedDate = new Date();

    private Long rollBackOrder;

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

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setRollBackOrder(Long rollBackOrder) {
        this.rollBackOrder = rollBackOrder;
    }

    public Long getRollBackOrder() {
        return rollBackOrder;
    }
}
