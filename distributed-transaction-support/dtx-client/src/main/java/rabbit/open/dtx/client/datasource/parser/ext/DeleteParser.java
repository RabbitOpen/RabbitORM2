package rabbit.open.dtx.client.datasource.parser.ext;

import rabbit.open.dtx.client.datasource.parser.Parser;
import rabbit.open.dtx.client.datasource.parser.SQLStructure;

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
        if (-1 != where) {
            structure.setCondition(structure.getFormattedSql().substring(where));
        } else {
            where = upperCaseSql.length();
        }
        // 表名
        structure.setTargetTables(structure.getFormattedSql().substring(from + "FROM".length(), where).trim());
    }

}
