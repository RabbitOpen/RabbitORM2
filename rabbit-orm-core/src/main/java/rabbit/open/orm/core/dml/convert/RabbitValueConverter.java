package rabbit.open.orm.core.dml.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import rabbit.open.orm.core.annotation.Column;
import rabbit.open.orm.core.dml.convert.ext.BigDecimalConverter;
import rabbit.open.orm.core.dml.convert.ext.BigIntegerConverter;
import rabbit.open.orm.core.dml.convert.ext.DateConverter;
import rabbit.open.orm.core.dml.convert.ext.DoubleConverter;
import rabbit.open.orm.core.dml.convert.ext.FloatConverter;
import rabbit.open.orm.core.dml.convert.ext.IntegerConverter;
import rabbit.open.orm.core.dml.convert.ext.LongConverter;
import rabbit.open.orm.core.dml.convert.ext.ShortConverter;
import rabbit.open.orm.core.dml.convert.ext.StringConverter;
import rabbit.open.orm.core.dml.meta.FieldMetaData;
import rabbit.open.orm.core.spring.SpringDaoAdapter;

/**
 * 
 * <b>@description 抽象值转换器 
 * 		1、将所有的正则类型进行值匹配
 * 		2、将所有的集合、数组转成list 
 * 		3、将外键对象的主键抽取出来进行匹配
 * </b>
 */
public abstract class RabbitValueConverter<T> {

	protected Class<T> clz;

	@SuppressWarnings("rawtypes")
	private static Map<Class<?>, RabbitValueConverter> converterCache = new ConcurrentHashMap<>();

	public RabbitValueConverter() {
		clz = SpringDaoAdapter.getTemplateClz(getClass());
	}

	/**
	 * <b>@description根据DB类型进行java值类型到DB数据值类型的转换 </b>
	 * @param value
	 * @param field
	 * @param isReg 是否是正则表达式
	 * @return
	 * 
	 */
	protected Object convert(Object value, FieldMetaData field, boolean isReg) {
		if (null == value) {
			return null;
		}
		if (field.isForeignKey()) {
			return convert(value, new FieldMetaData(field.getForeignField(), 
					field.getForeignField().getAnnotation(Column.class)), isReg);
		}
		return doConvert(value, field, isReg);
	}

	/**
	 * <b>@description 进行值转换 </b>
	 * @param value
	 * @param field
	 * @param isReg
	 * @return
	 */
	protected abstract Object doConvert(Object value, FieldMetaData field, boolean isReg);

	/**
	 * <b>@description 数字类型转换 </b>
	 * @param value
	 * @param field
	 * @return
	 */
	protected Object doNumericalConversion(Object value, FieldMetaData field) {
		if (isCollectionType(value)) {
			return convertArray(value);
		}
		if (!value.getClass().equals(field.getField().getType())) {
			return doCast(value);
		}
		return value;
	}

	protected boolean isCollectionType(Object value) {
		return value instanceof Collection || value.getClass().isArray();
	}

	protected Object convertArray(Object value) {
		if (value instanceof Collection) {
			return value;
		}
		List<Object> list = new ArrayList<>();
		Object[] arr = (Object[]) value;
		for (Object v : arr) {
			list.add(v);
		}
		return list;
	}

	/**
	 * <b>@description强制把data转换成T类型 </b>
	 * @return
	 */
	protected abstract T doCast(Object data);

	/**
	 * <b>@description 值转换 </b>
	 * @param <T>
	 * @param data
	 * @param type
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public static <T> T cast(Object data, Class<T> type) {
		makesureConverterInitialized();
		return (T) converterCache.get(type).doCast(data);
	}

	protected static void makesureConverterInitialized() {
		if (converterCache.isEmpty()) {
			converterCache.put(Long.class, new LongConverter());
			converterCache.put(Short.class, new ShortConverter());
			converterCache.put(Integer.class, new IntegerConverter());
			converterCache.put(Double.class, new DoubleConverter());
			converterCache.put(Float.class, new FloatConverter());
			converterCache.put(BigDecimal.class, new BigDecimalConverter());
			converterCache.put(BigInteger.class, new BigIntegerConverter());
			converterCache.put(Date.class, new DateConverter());
			converterCache.put(String.class, new StringConverter());
		}
	}

	/**
	 * <b>@description根据DB类型进行java值类型到DB数据值类型的转换 </b>
	 * @param value
	 * @param field
	 * @param isReg
	 * @return
	 * 
	 */
	public static Object convertByField(Object value, FieldMetaData field, boolean isReg) {
		makesureConverterInitialized();
		RabbitValueConverter<?> converter = converterCache.get(field.getField().getType());
		if (null == converter) {
			return value;
		}
		return converter.convert(value, field, isReg);
	}

	public static Object convertByField(Object value, FieldMetaData field) {
		return convertByField(value, field, false);
	}
}
