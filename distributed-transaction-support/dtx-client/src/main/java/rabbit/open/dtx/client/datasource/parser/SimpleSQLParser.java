package rabbit.open.dtx.client.datasource.parser;

import rabbit.open.dtx.client.datasource.parser.ext.DeleteParser;
import rabbit.open.dtx.client.datasource.parser.ext.InsertParser;
import rabbit.open.dtx.client.datasource.parser.ext.SelectParser;
import rabbit.open.dtx.client.datasource.parser.ext.UpdateParser;

import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * sql解析器, 只分析 update， insert， delete
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
public class SimpleSQLParser {

    private static Map<String, SQLMeta> structureMap = new ConcurrentHashMap<>();

    private static Map<SQLType, Parser> sqlParser = new EnumMap<>(SQLType.class);

    static {
        sqlParser.put(SQLType.INSERT, new InsertParser());
        sqlParser.put(SQLType.SELECT, new SelectParser());
        sqlParser.put(SQLType.UPDATE, new UpdateParser());
        sqlParser.put(SQLType.DELETE, new DeleteParser());
    }

    private SimpleSQLParser() {
    }

    /**
     * 进行简单的sql解析
     * @param    sql
     * @author xiaoqianbin
     * @date 2019/12/3
     **/
    public static SQLMeta parse(String sql) {
        if (structureMap.containsKey(sql)) {
            return structureMap.get(sql);
        }
        SQLMeta sqlMeta = new SQLMeta();
        // 去除多余的空白
        String formattedSql = sql.trim().replaceAll("\\s+", " ");
        sqlMeta.setFormattedSql(formattedSql);
        String upperCase = formattedSql.toUpperCase();
        setSqlType(sqlMeta, upperCase);
        sqlParser.get(sqlMeta.getSqlType()).parse(sqlMeta, upperCase);
        structureMap.put(sql, sqlMeta);
        return sqlMeta;
    }

    private static void setSqlType(SQLMeta sqlMeta, String upperCase) {
        if (upperCase.startsWith(SQLType.UPDATE.name())) {
            sqlMeta.setSqlType(SQLType.UPDATE);
        } else if (upperCase.startsWith(SQLType.DELETE.name())) {
            sqlMeta.setSqlType(SQLType.DELETE);
        } else if (upperCase.startsWith(SQLType.INSERT.name())) {
            sqlMeta.setSqlType(SQLType.INSERT);
        } else {
            sqlMeta.setSqlType(SQLType.SELECT);
        }
    }
}
