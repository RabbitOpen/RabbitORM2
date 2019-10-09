package rabbit.open.orm.core.utils;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import rabbit.open.orm.common.annotation.Column;
import rabbit.open.orm.core.dml.meta.SQLQueryFieldMeta;

public abstract class ClassHelper {

	private static Map<Class<?>, Map<String, SQLQueryFieldMeta>> cache = new ConcurrentHashMap<>();

	private ClassHelper() {
	}

	/**
	 * 
	 * <b>@description 获取类字段信息 </b>
	 * @param clz
	 * @param fieldName
	 * @return
	 */
	public static Field getField(Class<?> clz, String fieldName) {
		if (!cache.containsKey(clz)) {
			doCache(clz);
		}
		if (cache.get(clz).containsKey(fieldName)) {
			return cache.get(clz).get(fieldName).getField();
		}
		return null;
	}

	private static void doCache(Class<?> clz) {
		Class<?> target = clz;
		Map<String, SQLQueryFieldMeta> map = new ConcurrentHashMap<>();
		while (!Object.class.equals(target)) {
			for (Field f : target.getDeclaredFields()) {
				Column column = f.getAnnotation(Column.class);
				map.put(f.getName(), new SQLQueryFieldMeta(f, null == column ? null : column.value()));
			}
			target = target.getSuperclass();
		}
		cache.put(clz, map);
	}

	/**
	 * 
	 * <b>@description 获取类字段信息 </b>
	 * @param clz
	 * @param fieldName
	 * @return
	 */
	public static List<SQLQueryFieldMeta> getColumnFields(Class<?> clz) {
		if (!cache.containsKey(clz)) {
			doCache(clz);
		}
		return cache.get(clz).values().stream().filter(meta -> null != meta.getColumn())
				.collect(Collectors.toList());
	}
}