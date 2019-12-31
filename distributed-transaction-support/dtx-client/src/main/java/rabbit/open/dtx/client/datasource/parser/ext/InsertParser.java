package rabbit.open.dtx.client.datasource.parser.ext;

import rabbit.open.dtx.client.datasource.parser.ColumnMeta;
import rabbit.open.dtx.client.datasource.parser.Parser;
import rabbit.open.dtx.client.datasource.parser.SQLMeta;

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
    public void parse(SQLMeta sqlMeta, String upperCaseSql) {
        setTableName(sqlMeta, upperCaseSql);
        int values = upperCaseSql.indexOf(VALUES);
        setColumns(sqlMeta, upperCaseSql, values);
    }

    private void setColumns(SQLMeta sqlMeta, String upperCaseSql, int values) {
        String columnsSegment = sqlMeta.getFormattedSql().substring(upperCaseSql.indexOf('('), values).trim();
        columnsSegment = columnsSegment.substring(1, columnsSegment.length() - 1);
        String valuesSegment = sqlMeta.getFormattedSql().substring(values + VALUES.length() + 1, upperCaseSql.length()).trim();
        valuesSegment = valuesSegment.substring(1, valuesSegment.lastIndexOf(')'));
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
        sqlMeta.setColumns(columns);
    }

    private void setTableName(SQLMeta structure, String upperCaseSql) {
        String sql = structure.getFormattedSql();
        if (upperCaseSql.startsWith(INSERT_INTO)) {
            structure.setTargetTables(sql.substring(INSERT_INTO.length(), sql.indexOf('(')).trim());
        } else {
            structure.setTargetTables(sql.substring(INSERT.length(), sql.indexOf('(')).trim());
        }
    }

}
