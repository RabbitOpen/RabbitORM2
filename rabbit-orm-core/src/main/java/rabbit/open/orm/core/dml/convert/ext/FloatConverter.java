package rabbit.open.orm.core.dml.convert.ext;

import rabbit.open.orm.core.dml.convert.RabbitValueConverter;
import rabbit.open.orm.core.dml.meta.FieldMetaData;

public class FloatConverter extends RabbitValueConverter<Float> {

	@Override
	public Float doCast(Object data) {
		return Float.parseFloat(data.toString());
	}

	@Override
	protected Object doConvert(Object value, FieldMetaData field, boolean isReg) {
		return doNumericalConversion(value, field);
	}
}
