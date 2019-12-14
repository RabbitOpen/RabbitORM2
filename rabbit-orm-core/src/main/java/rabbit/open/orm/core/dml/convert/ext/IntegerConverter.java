package rabbit.open.orm.core.dml.convert.ext;

import rabbit.open.orm.core.dml.convert.RabbitValueConverter;
import rabbit.open.orm.core.dml.meta.FieldMetaData;

public class IntegerConverter extends RabbitValueConverter<Integer> {

	@Override
	public Integer doCast(Object data) {
		return Integer.parseInt(data.toString());
	}

	@Override
	protected Object doConvert(Object value, FieldMetaData field, boolean isReg) {
		return doNumericalConversion(value, field);
	}
}
