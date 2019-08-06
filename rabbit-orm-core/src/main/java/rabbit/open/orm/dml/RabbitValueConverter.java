package rabbit.open.orm.dml;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import rabbit.open.common.annotation.Column;
import rabbit.open.common.exception.RabbitDMLException;
import rabbit.open.orm.dml.meta.FieldMetaData;

/**
 * 
 * DB字段值转换
 * 
 * @author 肖乾斌
 * 
 */
public class RabbitValueConverter {

	/**
	 * 
	 * 根据DB类型进行java值类型到DB数据值类型的转换
	 * @param dbType
	 * @param value
	 * @param field
	 * @return
	 * 
	 */
	public static Object convert(Object value, FieldMetaData field) {
		return convert(value, field, false);
	}

	/**
	 * <b>@description 根据DB类型进行java值类型到DB数据值类型的转换 </b>
	 * @param value
	 * @param field
	 * @param isReg
	 * @return
	 */
	public static Object convert(Object value, FieldMetaData field,
			boolean isReg) {
		if (null == value) {
			return value;
		}
		if (field.isForeignKey()) {
			return convert(value, new FieldMetaData(field.getForeignField(),
					field.getForeignField().getAnnotation(Column.class)));
		}
		if (field.isNumerical()) {
			if (value instanceof Collection || value.getClass().isArray()) {
				return convertArray(value);
			}
			if (!value.getClass().equals(field.getField().getType())) {
				return cast(new BigDecimal(value.toString()), field.getField()
						.getType());
			}
			return value;
		}
		if (field.isString()) {
			if (value instanceof Collection || value.getClass().isArray()) {
				return convertArray(value);
			}
			return value;
		}

		if (field.isDate()) {
			return getDate(value, field, isReg);

		}
		return value;
	}

	private static Object convertArray(Object value) {
		if (value instanceof Collection) {
			return value;
		}
		List<Object> list = new ArrayList<>();
		if (value.getClass().isArray()) {
			Object[] arr = (Object[]) value;
			for (Object v : arr) {
				list.add(v);
			}
			return list;
		}
		list.add(value);
		return list;
	}

	@SuppressWarnings("unchecked")
	private static Object getDate(Object value, FieldMetaData field,
			boolean isReg) {
		if (null == value) {
			return null;
		}
		if (value instanceof Collection || value.getClass().isArray()) {
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

	private static Object convertDate(Object value, FieldMetaData field,
			boolean isReg) {
		SimpleDateFormat sdf = new SimpleDateFormat(field.getColumn().pattern());
		if (value instanceof Date) {
			String ds = sdf.format(value);
			try {
				return sdf.parse(ds);
			} catch (ParseException e) {
				return null;
			}
		} else {
			if (isReg) {
				return value;
			}
			throw new RabbitDMLException("object[" + value
					+ "] is not a instance of " + Date.class.getName());
		}
	}

	/**
	 * 
	 * <b>Description: 值转换</b><br>
	 * @param data
	 * @param type
	 * @return
	 * 
	 */
	public static Number cast(BigDecimal data, Class<?> type) {
		if (Long.class.equals(type)) {
			return data.longValue();
		}
		if (Integer.class.equals(type)) {
			return data.intValue();
		}
		if (Short.class.equals(type)) {
			return data.shortValue();
		}
		if (Double.class.equals(type)) {
			return data.doubleValue();
		}
		if (Float.class.equals(type)) {
			return data.floatValue();
		}
		if (BigInteger.class.equals(type)) {
			return data.toBigInteger();
		}
		if (BigDecimal.class.equals(type)) {
			return data;
		}
		throw new RabbitDMLException("not supported data type[" + type + "]");
	}

}
