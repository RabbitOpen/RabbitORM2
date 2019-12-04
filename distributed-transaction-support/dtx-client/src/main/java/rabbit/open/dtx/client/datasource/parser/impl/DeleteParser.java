package rabbit.open.dtx.client.datasource.parser.impl;

import rabbit.open.dtx.client.datasource.parser.Parser;
import rabbit.open.dtx.client.datasource.parser.SQLStructure;
import rabbit.open.dtx.client.datasource.parser.SQLType;

/**
 * delete parser
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
public class DeleteParser implements Parser {

    @Override
    public void parse(SQLStructure structure, String upperCaseSql) {
        int where = upperCaseSql.indexOf("WHERE");
        int from = upperCaseSql.indexOf("FROM");
        // condition
        structure.setCondition(structure.getFormattedSql().substring(where));
        // 表名
        structure.setTargetTables(structure.getFormattedSql().substring(from + "FROM".length(), where).trim());
    }
    
}
