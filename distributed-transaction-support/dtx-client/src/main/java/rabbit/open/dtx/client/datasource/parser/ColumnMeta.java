package rabbit.open.dtx.client.datasource.parser;

/**
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class ColumnMeta {

    private String columnName;

    private String value;

    // '?'占位符在整个sql中的index
    private int placeHolderIndex;

    // 真实值
    private Object realValue;

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

    public Object getRealValue() {
        return realValue;
    }

    public int getPlaceHolderIndex() {
        return placeHolderIndex;
    }
}
