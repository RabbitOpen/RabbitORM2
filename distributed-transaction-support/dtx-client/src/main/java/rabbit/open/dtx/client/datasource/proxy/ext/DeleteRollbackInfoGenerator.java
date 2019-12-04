package rabbit.open.dtx.client.datasource.proxy.ext;

import rabbit.open.dtx.client.datasource.parser.SQLMeta;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Delete回滚信息生成器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class DeleteRollbackInfoGenerator extends UpdateRollbackInfoGenerator {

    @Override
    protected void setPlaceHolderValues(SQLMeta sqlMeta, List<Object> preparedStatementValues, PreparedStatement stmt) throws SQLException {
        for (int i = 1; i <= preparedStatementValues.size(); i++) {
            setPreparedStatementValue(stmt, i, preparedStatementValues.get(i - 1));
        }
    }
}
