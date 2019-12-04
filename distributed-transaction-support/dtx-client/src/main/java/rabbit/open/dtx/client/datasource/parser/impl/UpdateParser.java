package rabbit.open.dtx.client.datasource.parser.impl;

import rabbit.open.dtx.client.datasource.parser.Parser;
import rabbit.open.dtx.client.datasource.parser.SQLStructure;
import rabbit.open.dtx.client.datasource.parser.SQLType;
import rabbit.open.dtx.client.datasource.parser.SimpleSQLParser;

import java.util.ArrayList;
import java.util.List;

/**
 * update parser
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
public class UpdateParser implements Parser {

    @Override
    public void parse(SQLStructure structure, String upperCaseSql) {
        int where = upperCaseSql.indexOf("WHERE");
        int set = upperCaseSql.indexOf("SET");
        // where
        structure.setCondition(structure.getFormattedSql().substring(where));
        // 表名
        structure.setTargetTables(structure.getFormattedSql().substring(SQLType.UPDATE.name().length(), set).trim());
        setColumns(structure, upperCaseSql, where, set);
    }

    /**
     * 设置要更新的字段信息
     * @param	structure
	 * @param	upperCaseSql
	 * @param	where
	 * @param	set
     * @author  xiaoqianbin
     * @date    2019/12/3
     **/
    private void setColumns(SQLStructure structure, String upperCaseSql, int where, int set) {
        String updateColumns = structure.getFormattedSql().substring(set + "SET".length(), where).trim();
        List<String> list = new ArrayList<>();
        for (String column : updateColumns.split(",")) {
            list.add(column.split("=")[0].trim());
        }
        structure.setColumns(list);
    }


}
