package rabbit.open.dtx.client.datasource.parser.impl;

import rabbit.open.dtx.client.datasource.parser.Parser;
import rabbit.open.dtx.client.datasource.parser.SQLStructure;

import java.util.ArrayList;
import java.util.List;

/**
 * insert parser
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
public class InsertParser implements Parser {

    public static final String INSERT_INTO = "INSERT INTO";
    public static final String INSERT = "INSERT";

    @Override
    public void parse(SQLStructure structure, String upperCaseSql) {
        setTableName(structure, upperCaseSql);
        int values = upperCaseSql.indexOf("VALUES");
        setColumns(structure, upperCaseSql, values);
    }

    private void setColumns(SQLStructure structure, String upperCaseSql, int values) {
        String column = structure.getFormattedSql().substring(upperCaseSql.indexOf("("), values).trim();
        column = column.substring(1, column.length() - 1);
        List<String> columns = new ArrayList<>();
        for (String c : column.split(",")) {
            columns.add(c.trim());
        }
        structure.setColumns(columns);
    }

    private void setTableName(SQLStructure structure, String upperCaseSql) {
        String sql = structure.getFormattedSql();
        if (upperCaseSql.startsWith(INSERT_INTO)) {
            structure.setTargetTables(sql.substring(INSERT_INTO.length(), sql.indexOf("(")).trim());
        } else if (upperCaseSql.startsWith(INSERT)) {
            structure.setTargetTables(sql.substring(INSERT.length(), sql.indexOf("(")).trim());
        }
    }

}
