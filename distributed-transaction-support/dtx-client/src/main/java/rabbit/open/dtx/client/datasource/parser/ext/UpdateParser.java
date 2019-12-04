package rabbit.open.dtx.client.datasource.parser.ext;

import rabbit.open.dtx.client.datasource.parser.ColumnMeta;
import rabbit.open.dtx.client.datasource.parser.Parser;
import rabbit.open.dtx.client.datasource.parser.SQLStructure;
import rabbit.open.dtx.client.datasource.parser.SQLType;

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
        if (-1 != where) {
            structure.setCondition(structure.getFormattedSql().substring(where));
        } else {
            where = upperCaseSql.length();
        }
        // 表名
        structure.setTargetTables(structure.getFormattedSql().substring(SQLType.UPDATE.name().length(), set).trim());
        setColumns(structure, where, set);
    }

    /**
     * 设置要更新的字段信息
     * @param	structure
	 * @param	where
	 * @param	set
     * @author  xiaoqianbin
     * @date    2019/12/3
     **/
    private void setColumns(SQLStructure structure, int where, int set) {
        String updateColumns = structure.getFormattedSql().substring(set + "SET".length(), where).trim();
        List<ColumnMeta> list = new ArrayList<>();
        int placeHolderIndex = 0;
        for (String column : updateColumns.split(",")) {
            String value = column.split("=")[1].trim();
            list.add(new ColumnMeta(column.split("=")[0].trim(), value, placeHolderIndex));
            if ("?".equals(value)) {
                placeHolderIndex++;
            }
        }
        structure.setColumns(list);
    }

}
