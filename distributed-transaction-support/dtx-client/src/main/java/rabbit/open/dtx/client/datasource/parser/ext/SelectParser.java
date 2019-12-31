package rabbit.open.dtx.client.datasource.parser.ext;

import rabbit.open.dtx.client.datasource.parser.Parser;
import rabbit.open.dtx.client.datasource.parser.SQLMeta;

/**
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
public class SelectParser implements Parser {

    @Override
    public void parse(SQLMeta sqlMeta, String upperCaseSql) {
        // TO DO: NOTING, ignore all select
    }

}
