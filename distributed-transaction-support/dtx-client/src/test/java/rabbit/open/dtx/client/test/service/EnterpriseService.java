package rabbit.open.dtx.client.test.service;

import junit.framework.TestCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rabbit.open.dtx.client.test.entity.Enterprise;

/**
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
@Service
public class EnterpriseService extends GenericService<Enterprise> {

    private int count = 0;

    public void increase() {
        count++;
    }

    public int getCount() {
        return count;
    }

    @Transactional
    public void addEnterprise(Enterprise enterprise) {
        // count 在进入方法之前已经被修改了
        TestCase.assertEquals(1, count);
        add(enterprise);
    }

}
