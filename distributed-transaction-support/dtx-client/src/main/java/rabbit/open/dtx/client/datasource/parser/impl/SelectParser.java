package rabbit.open.dtx.client.datasource.parser.impl;

import rabbit.open.dtx.client.datasource.parser.Parser;
import rabbit.open.dtx.client.datasource.parser.SQLStructure;

/**
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
public class SelectParser implements Parser {

    @Override
    public void parse(SQLStructure structure, String upperCaseSql) {
        // TO DO: NOTING, ignore all select
    }
    
}
