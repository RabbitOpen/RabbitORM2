package rabbit.open.dtx.client.datasource.parser;

/**
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class ColumnMeta {

    private String columnName;

    // sql中column对应的值
    private String value;

    // '?'占位符在整个sql中的index
    private int placeHolderIndex;

    public ColumnMeta() {
    }

    public ColumnMeta(String columnName, String value, int placeHolderIndex) {
        this.columnName = columnName;
        this.value = value;
        this.placeHolderIndex = placeHolderIndex;
    }

    public String getColumnName() {
        return columnName;
    }

    public String getValue() {
        return value;
    }

    public int getPlaceHolderIndex() {
        return placeHolderIndex;
    }
}
