package rabbit.open.orm.core.dml.convert.ext;

import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.dml.convert.RabbitValueConverter;
import rabbit.open.orm.core.dml.meta.FieldMetaData;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class DateConverter extends RabbitValueConverter<Date> {

	@Override
	protected Object doConvert(Object value, FieldMetaData field, boolean isReg) {
		return doDateConversion(value, field, isReg);
	}

	@SuppressWarnings("unchecked")
	private Object doDateConversion(Object value, FieldMetaData field, boolean isReg) {
		if (isCollectionType(value)) {
			List<Object> list = new ArrayList<>();
			list.addAll((Collection<Object>) convertArray(value));
			List<Object> dates = new ArrayList<>();
			for (Object v : list) {
				dates.add(convertDate(v, field, isReg));
			}
			return dates;
		}
		return convertDate(value, field, isReg);
	}

	private Object convertDate(Object value, FieldMetaData field, boolean isReg) {
		SimpleDateFormat sdf = new SimpleDateFormat(field.getColumn().pattern());
		if (value instanceof Date) {
			String ds = sdf.format(value);
			try {
				return sdf.parse(ds);
			} catch (ParseException e) {
				return null;
			}
		} else {
			// 两个日期计算差值时，值为整数，类型则不匹配
			if (isReg) {
				return value;
			}
			throw new RabbitDMLException("object[" + value + "] is not a instance of " + Date.class.getName());
		}
	}

	@Override
	protected Date doCast(Object data) {
		return (Date) data;
	}

}
