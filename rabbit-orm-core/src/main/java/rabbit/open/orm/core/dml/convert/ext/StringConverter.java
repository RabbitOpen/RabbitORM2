package rabbit.open.orm.core.dml.convert.ext;

import rabbit.open.orm.core.dml.convert.RabbitValueConverter;
import rabbit.open.orm.core.dml.meta.FieldMetaData;

public class StringConverter extends RabbitValueConverter<String> {

	@Override
	protected Object doConvert(Object value, FieldMetaData field, boolean isReg) {
		return doStringConversion(value);
	}

	private Object doStringConversion(Object value) {
		if (isCollectionType(value)) {
			return convertArray(value);
		}
		return value;
	}

	@Override
	protected String doCast(Object data) {
		return data.toString();
	}

}
