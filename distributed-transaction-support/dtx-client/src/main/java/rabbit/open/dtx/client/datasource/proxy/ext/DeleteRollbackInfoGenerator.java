package rabbit.open.dtx.client.datasource.proxy.ext;

import rabbit.open.dtx.client.datasource.parser.SQLMeta;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfo;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfoGenerator;

import java.util.List;

/**
 * 删除操作回滚数据生成器
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class DeleteRollbackInfoGenerator extends RollbackInfoGenerator {

    @Override
    public RollbackInfo generate(SQLMeta sqlMeta, List<Object> preparedStatementValues) {
        return null;
    }
}
