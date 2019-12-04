package rabbit.open.dtx.client.datasource.parser;

import java.util.List;

/**
 * sql结构说明
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
public class SQLStructure {

    // sql 类型
    private SQLType sqlType;

    // sql语句
    private String formattedSql;

    // 条件
    private String condition;

    // 目标表
    private String targetTables;

    // 字段信息
    private List<String> columns;

    public SQLType getSqlType() {
        return sqlType;
    }

    public void setSqlType(SQLType sqlType) {
        this.sqlType = sqlType;
    }

    public String getFormattedSql() {
        return formattedSql;
    }

    public void setFormattedSql(String formattedSql) {
        this.formattedSql = formattedSql;
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public String getTargetTables() {
        return targetTables;
    }

    public void setTargetTables(String targetTables) {
        this.targetTables = targetTables;
    }

    public List<String> getColumns() {
        return columns;
    }

    public void setColumns(List<String> columns) {
        this.columns = columns;
    }
}
