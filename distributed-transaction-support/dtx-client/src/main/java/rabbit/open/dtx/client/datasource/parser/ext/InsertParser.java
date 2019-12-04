package rabbit.open.dtx.client.datasource.parser.ext;

import rabbit.open.dtx.client.datasource.parser.ColumnMeta;
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
    public static final String VALUES = "VALUES";

    @Override
    public void parse(SQLStructure structure, String upperCaseSql) {
        setTableName(structure, upperCaseSql);
        int values = upperCaseSql.indexOf(VALUES);
        setColumns(structure, upperCaseSql, values);
    }

    private void setColumns(SQLStructure structure, String upperCaseSql, int values) {
        String columnsSegment = structure.getFormattedSql().substring(upperCaseSql.indexOf('('), values).trim();
        columnsSegment = columnsSegment.substring(1, columnsSegment.length() - 1);
        String valuesSegment = structure.getFormattedSql().substring(values + VALUES.length() + 1, upperCaseSql.lastIndexOf(')'));
        List<ColumnMeta> columns = new ArrayList<>();
        int placeHolderIndex = 0;
        String[] valueSegments = valuesSegment.split(",");
        String[] columnSegments = columnsSegment.split(",");
        for (int i = 0; i < columnSegments.length; i++) {
            String column = columnSegments[i];
            columns.add(new ColumnMeta(column.trim(), valueSegments[i].trim(), placeHolderIndex));
            if ("?".equals(valueSegments[i].trim())) {
                placeHolderIndex++;
            }
        }
        structure.setColumns(columns);
    }

    private void setTableName(SQLStructure structure, String upperCaseSql) {
        String sql = structure.getFormattedSql();
        if (upperCaseSql.startsWith(INSERT_INTO)) {
            structure.setTargetTables(sql.substring(INSERT_INTO.length(), sql.indexOf('(')).trim());
        } else if (upperCaseSql.startsWith(INSERT)) {
            structure.setTargetTables(sql.substring(INSERT.length(), sql.indexOf('(')).trim());
        }
    }

}
