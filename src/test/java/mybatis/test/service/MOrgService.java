package mybatis.test.service;

import mybatis.test.entity.MOrg;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MOrgService extends BaseService<MOrg> {

	@Transactional(propagation = Propagation.NESTED)
	public void nestedTranAdd(String name) {
		add(new MOrg(name));
	}
}
