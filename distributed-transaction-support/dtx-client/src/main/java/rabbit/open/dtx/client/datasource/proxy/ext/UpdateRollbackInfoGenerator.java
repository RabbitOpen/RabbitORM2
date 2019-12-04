package rabbit.open.dtx.client.datasource.proxy.ext;

import rabbit.open.dtx.client.context.DistributedTransactionObject;
import rabbit.open.dtx.client.datasource.parser.SQLStructure;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfo;
import rabbit.open.dtx.client.datasource.proxy.RollbackInfoGenerator;

import java.util.List;

/**
 * @author xiaoqianbin
 * @date 2019/12/4
 **/
public class UpdateRollbackInfoGenerator extends RollbackInfoGenerator {

    @Override
    public RollbackInfo generate(DistributedTransactionObject context, SQLStructure sqlStructure, List<Object> preparedStatementValues) {
        return null;
    }
}
