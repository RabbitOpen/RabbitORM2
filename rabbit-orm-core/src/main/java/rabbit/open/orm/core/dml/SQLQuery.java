package rabbit.open.orm.core.dml;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rabbit.open.orm.common.dml.DMLType;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.dml.meta.MetaData;
import rabbit.open.orm.core.dml.meta.SQLQueryFieldMeta;
import rabbit.open.orm.core.utils.ClassHelper;
import rabbit.open.orm.datasource.Session;

/**
 * 
 * jdbc命名sql是原生sql，查询出来的数据通过字段名和对象属性进行匹配 
 * <b>@description jdbc命名sql查询对象 </b>
 * @param <T>
 */
public class SQLQuery<T> {

	private Query<T> query;
	
	private SessionFactory factory;
	
	// 字段变量
	private static final String FIELDS_REPLACE_WORD = "\\#\\{(.*?)\\}";
	
	// 分区表变量
	protected static final String TABLE_REPLACE_WORD = "\\@\\{(.*?)\\}";
	
	// 返回结果的clz
	private Class<?> resultClz;
	
	/**
	 * @param sessionFactory
	 * @param clz			 需要返回的对象
	 * @param queryName
	 */
	public SQLQuery(SessionFactory sessionFactory, Class<T> clz, String queryName) {
		this.factory = sessionFactory;
		query = new Query<T>(sessionFactory, clz) {
			
			// 重新创建sql语句
			@Override
			protected void createQuerySql() {
				if (query.getMetaData().isShardingTable()) {
					query.addedFilters.clear();
					query.runCallBackTask();
					query.combineFilters();
				}
				sql = new StringBuilder(query.namedObject.getSql());
				replaceFieldsSql();
				replaceTableName();
				setPreparedValues();
				createPageSql();
			}

			// 用真实的分区表名替换表名占位符
			private void replaceTableName() {
				Pattern pattern = Pattern.compile(TABLE_REPLACE_WORD);
		        Matcher matcher = pattern.matcher(sql.toString());
		        if (matcher.find()) {
		        	sql = new StringBuilder(sql.toString().replace(matcher.group(0), getCurrentTableName()));
		        }
			}

			// 替换sql语句中的 #{fields}部分
			private void replaceFieldsSql() {
				Pattern pattern = Pattern.compile(FIELDS_REPLACE_WORD);
		        Matcher matcher = pattern.matcher(sql.toString());
		        if (matcher.find()) {
		        	sql = new StringBuilder(sql.toString().replace(matcher.group(0), getFieldsSql().toString()));
		        }
			}

			@Override
			protected void createCountSql() {
				String sqlStr = query.namedObject.getSql().toLowerCase().trim().replaceAll("\\s+", " ");
				sqlStr = "SELECT COUNT(1) " + query.namedObject.getSql().substring(sqlStr.indexOf("from"));
				sql = new StringBuilder(sqlStr);
				setPreparedValues();
			}

			@Override
			protected String getCurrentTableName() {
				if (getMetaData().isShardingTable()) {
					return super.getCurrentTableName();
				}
				return getCurrentTableNameByNamedObject(namedObject);
			}
		};
		query.namedObject = query.getSessionFactory().getQueryByNameAndClass(queryName, clz);
		query.fieldsValues = new TreeMap<>((o1, o2) -> o1.compareTo(o2));
	}

	/***
	 * 
	 * <b>@description 获取结果集对应的sql片段信息 </b>
	 * @return
	 */
	private StringBuilder getFieldsSql() {
		List<SQLQueryFieldMeta> fields = ClassHelper.getColumnFields(getResultClz());
		StringBuilder sb = new StringBuilder();
		for (SQLQueryFieldMeta f : fields) {
			sb.append(query.namedObject.getAlias() + "." + f.getColumn() + " as " + f.getField().getName() + ",");
		}
		if (sb.length() > 0) {
			sb.deleteCharAt(sb.length() - 1);
		}
		return sb;
	}
	
	private <D> Object execute(Class<D> clz) {
		setResultClz(clz);
		query.createQuerySql();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = factory.getConnection(query.getEntityClz(), query.getCurrentTableName(), DMLType.SELECT);
			stmt = conn.prepareStatement(query.sql.toString());
			query.setPreparedStatementValue(stmt, DMLType.SELECT);
			query.showSql();
			rs = stmt.executeQuery();
			List<D> resultList = readResults(rs, clz);
			rs.close();
			return resultList;
		} catch (Exception e) {
			query.showUnMaskedSql(false);
			Session.flagException();
			throw new RabbitDMLException(e.getMessage(), e);
		} finally {
			query.closeResultSet(rs);
			DMLObject.closeStmt(stmt);
			DMLObject.closeConnection(conn);
			Session.clearException();
		}
	}

	private <D> void setResultClz(Class<D> clz) {
		this.resultClz = clz;
	}
	
	private Class<?> getResultClz() {
		return resultClz;
	}

	/**
	 * <b>@description 准备预编译sql的值 </b>
	 */
	protected void setPreparedValues() {
		if (getFieldsValues().isEmpty()) {
            return;
        }
		query.preparedValues.clear();
        Collection<PreparedValue> values = getFieldsValues().values();
        for (PreparedValue v : values) {
            query.preparedValues.add(v);
        }
	}
	
	private TreeMap<Integer, PreparedValue> getFieldsValues() {
		return query.fieldsValues;
	}

	/**
	 * <b>Description      单个设值</b>
	 * @param fieldAlias   字段在sql中的别名
	 * @param value        字段的值
	 * @param fieldName    字段在对应实体中的名字  	 如果当前主表有分表时参数必传
	 * @param entityClz    字段所属的实体   			 如果当前主表有分表时参数必传
	 * @return
	 */
	public SQLQuery<T> set(String fieldAlias, Object value, String fieldName, Class<?> entityClz) {
		query.setVariable(fieldAlias, value, fieldName, entityClz);
		if (query.getEntityClz().equals(entityClz)) {
			query.addFilter(fieldName, value);
		}
	    return this;
	}

	/**
	 * <b>@description 读一行数据 </b>
	 * @return
	 */
	public T unique() {
		return unique(query.getEntityClz());
	}
	
	/**
	 * <b>@description 读一行数据 </b>
	 * @param <D>
	 * @param clz	需要转换的类型
	 * @return
	 */
	public <D> D unique(Class<D> clz) {
		List<D> list = list(clz);
		return list.isEmpty() ? null : list.get(0);
	}

	/**
	 * <b>@description 查询列表 </b>
	 * @return
	 */
	public List<T> list() {
		return list(query.getEntityClz());
	}
	
	/**
	 * <b>@description 查询列表 </b>
	 * @param <D>
	 * @param clz	需要转换的类型
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <D> List<D> list(Class<D> clz) {
		return (List<D>) execute(clz);
	}
	
	
	/**
	 * <b>@description 分页 </b>
	 * @param pageIndex
	 * @param pageSize
	 * @return
	 */
	public SQLQuery<T> page(int pageIndex, int pageSize) {
		query.page(pageIndex, pageSize);
		return this;
	}
	
	/**
	 * <b>@description 统计条数 </b>
	 * @return
	 */
	public long count() {
		query.createCountSql();
		Connection conn = null;
		PreparedStatement stmt = null;
		ResultSet rs = null;
		try {
			conn = factory.getConnection(query.getEntityClz(), query.getCurrentTableName(), DMLType.SELECT);
			stmt = conn.prepareStatement(query.sql.toString());
			query.setPreparedStatementValue(stmt, DMLType.SELECT);
			query.showSql();
			rs = stmt.executeQuery();
			rs.next();
			return Long.parseLong(rs.getObject(1).toString());
		} catch (Exception e) {
			query.showUnMaskedSql(false);
			Session.flagException();
			throw new RabbitDMLException(e.getMessage(), e);
		} finally {
			query.closeResultSet(rs);
			DMLObject.closeStmt(stmt);
			DMLObject.closeConnection(conn);
			Session.clearException();
		}
	}

	/**
	 * <b>@description 读取结果集 </b>
	 * @param rs
	 * @param clz
	 */
	private <D> List<D> readResults(ResultSet rs, Class<D> clz) throws SQLException, ReflectiveOperationException {
		List<D> list = new ArrayList<>();
		List<String> headers = getColumnNames(rs);
		while (rs.next()) {
			D targetObj = DMLObject.newInstance(clz);
			for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
				Object colValue = rs.getObject(i);
				if (null == colValue) {
					continue;
				}
				if (factory.getDialectType().isOracle() && colValue instanceof Date) {
					colValue = rs.getTimestamp(i);
				}
				Field field = ClassHelper.getField(clz, headers.get(i - 1));
				if (null != field) {
					setValue2Obj(targetObj, colValue, field);
				}
			}
			list.add(targetObj);
		}
		return list;
	}

	/**
	 * 
	 * <b>@description 往目标对象targetObj的field字段设值 </b>
	 * @param <D>
	 * @param targetObj
	 * @param colValue		数据库中查询出来的值
	 * @param field
	 */
	private <D> void setValue2Obj(D targetObj, Object colValue, Field field) throws ReflectiveOperationException {
		Object value = factory.onValueGot(colValue, field);
		if (MetaData.isEntityClass(field.getType())) {
			field.setAccessible(true);
			Object obj = DMLObject.newInstance(field.getType());
			field.set(targetObj, obj);
			DialectTransformer.getTransformer(factory.getDialectType()).setValue2EntityField(obj,
					MetaData.getPrimaryKeyField(field.getType()), value);
		} else {
			DialectTransformer.getTransformer(factory.getDialectType()).setValue2EntityField(targetObj,
					field, value);
		}
	}
	
	/**
	 * <b>@description 根据结果集获取列名信息 </b>
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private List<String> getColumnNames(ResultSet rs) throws SQLException {
		List<String> headers = new ArrayList<>();
		for (int i = 1; i <= rs.getMetaData().getColumnCount(); i++) {
			headers.add(rs.getMetaData().getColumnLabel(i));
		}
		return headers;
	}


}
