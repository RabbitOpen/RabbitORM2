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

    private static Map<String, SQLStructure> structureMap = new ConcurrentHashMap<>();

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
     * @param	sql
     * @author  xiaoqianbin
     * @date    2019/12/3
     **/
    public static SQLStructure parse(String sql) {
        if (structureMap.containsKey(sql)) {
            return structureMap.get(sql);
        }
        SQLStructure structure = new SQLStructure();
        // 去除多余的空白
        String formattedSql = sql.trim().replaceAll("\\s+", " ");
        structure.setFormattedSql(formattedSql);
        String upperCase = formattedSql.toUpperCase();
        setSqlType(structure, upperCase);
        sqlParser.get(structure.getSqlType()).parse(structure, upperCase);
        structureMap.put(sql, structure);
        return structure;
    }

    private static void setSqlType(SQLStructure structure, String upperCase) {
        if (upperCase.startsWith(SQLType.UPDATE.name())) {
            structure.setSqlType(SQLType.UPDATE);
        } else if (upperCase.startsWith(SQLType.DELETE.name())) {
            structure.setSqlType(SQLType.DELETE);
        } else if (upperCase.startsWith(SQLType.INSERT.name())) {
            structure.setSqlType(SQLType.INSERT);
        } else {
            structure.setSqlType(SQLType.SELECT);
        }
    }
}
