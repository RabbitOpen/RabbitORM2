package rabbit.open.dtx.client.datasource.parser;

/**
 * sql 解析
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
public interface Parser {

    void parse(SQLStructure structure, String upperCaseSql);
}
