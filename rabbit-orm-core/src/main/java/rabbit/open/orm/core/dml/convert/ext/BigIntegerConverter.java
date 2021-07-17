package rabbit.open.orm.core.dml.convert.ext;

import rabbit.open.orm.core.dml.convert.RabbitValueConverter;
import rabbit.open.orm.core.dml.meta.FieldMetaData;

import java.math.BigDecimal;
import java.math.BigInteger;

public class BigIntegerConverter extends RabbitValueConverter<BigInteger> {

	@Override
	public BigInteger doCast(Object data) {
		return new BigDecimal(data.toString()).toBigInteger();
	}

	@Override
	protected Object doConvert(Object value, FieldMetaData field, boolean isReg) {
		return doNumericalConversion(value, field);
	}
}
