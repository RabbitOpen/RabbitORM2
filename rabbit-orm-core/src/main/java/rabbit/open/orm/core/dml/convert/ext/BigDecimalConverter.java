package rabbit.open.orm.core.dml.convert.ext;

import java.math.BigDecimal;

import rabbit.open.orm.core.dml.convert.RabbitValueConverter;
import rabbit.open.orm.core.dml.meta.FieldMetaData;

public class BigDecimalConverter extends RabbitValueConverter<BigDecimal> {

	@Override
	public BigDecimal doCast(Object data) {
		return new BigDecimal(data.toString());
	}

	@Override
	protected Object doConvert(Object value, FieldMetaData field, boolean isReg) {
		return doNumericalConversion(value, field);
	}

}
