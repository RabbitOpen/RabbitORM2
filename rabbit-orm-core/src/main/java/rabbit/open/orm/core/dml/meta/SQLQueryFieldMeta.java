package rabbit.open.orm.core.dml.meta;

import java.lang.reflect.Field;

/**
 * 
 * <b>@description SQLQuery专用字段信息 </b>
 */
public class SQLQueryFieldMeta {

	private Field field;
	
	private String column;

	public SQLQueryFieldMeta(Field field, String column) {
		super();
		setColumn(column);
		setField(field);
	}

	public Field getField() {
		return field;
	}

	public void setField(Field field) {
		this.field = field;
	}

	public String getColumn() {
		return column;
	}

	public void setColumn(String column) {
		this.column = column;
	}
	
	
}
