package rabbit.open.orm.core.dml.convert.ext;

import rabbit.open.orm.core.dml.convert.RabbitValueConverter;
import rabbit.open.orm.core.dml.meta.FieldMetaData;

public class ShortConverter extends RabbitValueConverter<Short> {

	@Override
	public Short doCast(Object data) {
		return Short.parseShort(data.toString());
	}

	@Override
	protected Object doConvert(Object value, FieldMetaData field, boolean isReg) {
		return doNumericalConversion(value, field);
	}
}
