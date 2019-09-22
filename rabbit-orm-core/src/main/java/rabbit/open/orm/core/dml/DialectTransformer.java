package rabbit.open.orm.core.dml;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

import rabbit.open.orm.common.dialect.DialectType;
import rabbit.open.orm.common.exception.RabbitDMLException;
import rabbit.open.orm.core.dialect.dml.impl.DB2Transformer;
import rabbit.open.orm.core.dialect.dml.impl.MySQLTransformer;
import rabbit.open.orm.core.dialect.dml.impl.OracleTransformer;
import rabbit.open.orm.core.dialect.dml.impl.SQLServerTransformer;
import rabbit.open.orm.core.dialect.dml.impl.SQLite3Transformer;
import rabbit.open.orm.core.dialect.pager.Pager;
import rabbit.open.orm.core.dml.filter.PreparedValue;

/**
 * <b>Description: 	sql方言转换器</b><br>
 * <b>@author</b>	肖乾斌
 */
public abstract class DialectTransformer {

    private static Map<DialectType, DialectTransformer> cache = new ConcurrentHashMap<>();

    private Logger logger = Logger.getLogger(getClass());
    
    private Pager pager;
    
    // 获取pager
    public final Pager getPager() {
    	if (null == pager) {
    		pager = createPager();
    	}
    	return pager;
    }
    
    protected abstract Pager createPager();

    /**
     * <b>Description:  根据数据库的不同，将字段sql片段进行转换</b><br>.
     * @param query
     * @return
     */
    public StringBuilder completeFieldsSql(AbstractQuery<?> query) {
        StringBuilder sql = getSql(query);
        sql.insert(0, "SELECT ");
        return sql;
    }

    /**
     * <b>Description:	生成排序的sql</b><br>
     * @param query
     * @return
     */
    public StringBuilder createOrderSql(AbstractQuery<?> query) {
        Map<Class<?>, HashSet<String>> asc = getAsc(query);
        Map<Class<?>, HashSet<String>> desc = getDesc(query);
        StringBuilder sql = new StringBuilder();
        if (asc.isEmpty() && desc.isEmpty()) {
            return sql;
        }
        sql.append(" ORDER BY ");
        Iterator<Class<?>> it = asc.keySet().iterator();
        while (it.hasNext()) {
            Class<?> clz = it.next();
            HashSet<String> fields = asc.get(clz);
            for (String f : fields) {
                sql.append(query.getAliasByTableName(query.getTableNameByClass(clz)) + "." + f + " ASC, ");
            }
        }
        it = desc.keySet().iterator();
        while (it.hasNext()) {
            Class<?> clz = it.next();
            HashSet<String> fields = desc.get(clz);
            for (String f : fields) {
                sql.append(query.getAliasByTableName(query.getTableNameByClass(clz)) + "." + f + " DESC, ");
            }
        }
        sql.deleteCharAt(sql.lastIndexOf(","));
        return sql;
    }

    /**
     * <b>Description:  创建分页sql</b><br>.
     * @param query
     * @return
     */
    public abstract StringBuilder createPageSql(AbstractQuery<?> query);

    /**
     * <b>Description:	注册转换器</b><br>
     * @param dialect
     * @param transformer
     */
    private static void registTransformer(DialectType dialect, DialectTransformer transformer) {
        cache.put(dialect, transformer);
    }

    /**
     * <b>Description:	根据方言获取转换器</b><br>
     * @param dialect
     * @return
     */
    public static DialectTransformer getTransformer(DialectType dialect) {
        if (!cache.containsKey(dialect)) {
            throw new RabbitDMLException("unknown dialect[" + dialect + "] is found!");
        }
        return cache.get(dialect);
    }

    /**
     * 设置分页的起始截止标识
     * <b>@description  </b>
     * @param query
     */
	protected void setStartAndEndPreparedValues(AbstractQuery<?> query) {
		int pageSize = getPageSize(query);
        int pageIndex = getPageIndex(query);
        List<Object> preparedValues = getPreparedValues(query);
        long start = 1L + pageIndex * pageSize;
        long end = (1L + pageIndex) * pageSize;
        preparedValues.add(new PreparedValue(start));
        preparedValues.add(new PreparedValue(end));
	}
	
    /**
     * <b>Description  给指定对象的字段设值</b>
     * @param target
     * @param f
     * @param value
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void setValue2EntityField(Object target, Field f, Object value) {
        try {
            f.setAccessible(true);
            if (value instanceof Number) {
                f.set(target, RabbitValueConverter.cast(new BigDecimal(value.toString()), f.getType()));
            } else {
                if (Enum.class.isAssignableFrom(f.getType())) {
                    Class clz = f.getType();
                    f.set(target, Enum.valueOf(clz, value.toString()));
                } else {
                    f.set(target, value);
                }
            }
        } catch (Exception e) {
            logger.error("[" + target.getClass() + " -> " + f.getName() + "] value type error! value[" + value + "]");
            throw new RabbitDMLException(e.getMessage(), e);
        }
    }

    public static void init() {
        registTransformer(DialectType.MYSQL, new MySQLTransformer());
        registTransformer(DialectType.ORACLE, new OracleTransformer());
        registTransformer(DialectType.DB2, new DB2Transformer());
        registTransformer(DialectType.SQLSERVER, new SQLServerTransformer());
        registTransformer(DialectType.SQLITE3, new SQLite3Transformer());
    }

    public StringBuilder getSql(AbstractQuery<?> query) {
        return query.sql;
    }

    public int getPageIndex(AbstractQuery<?> query) {
        return query.pageIndex;
    }

    public int getPageSize(AbstractQuery<?> query) {
        return query.pageSize;
    }

    public Map<Class<?>, HashSet<String>> getDesc(AbstractQuery<?> query) {
        return query.desc;
    }

    public Map<Class<?>, HashSet<String>> getAsc(AbstractQuery<?> query) {
        return query.asc;
    }

    public List<Object> getPreparedValues(AbstractQuery<?> query) {
        return query.preparedValues;
    }

    public boolean doPage(AbstractQuery<?> query) {
        return query.page;
    }

    public boolean doOrder(AbstractQuery<?> query) {
        return !query.asc.isEmpty() || !query.desc.isEmpty();
    }

    public String getAliasByTableName(AbstractQuery<?> query, String tableName) {
        return query.getAliasByTableName(tableName);
    }

    public boolean distinct(AbstractQuery<?> query) {
        return query.distinct;
    }
}
