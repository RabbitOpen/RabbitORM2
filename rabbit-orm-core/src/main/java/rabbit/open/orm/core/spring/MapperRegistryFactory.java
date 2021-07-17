package rabbit.open.orm.core.spring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.*;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import rabbit.open.orm.core.annotation.NameSpace;
import rabbit.open.orm.core.utils.PackageScanner;

import java.util.Set;

/**
 * <b>@description mapper注册工厂 </b>
 */
public class MapperRegistryFactory implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

	private ApplicationContext context;
	
	private String rootPath;
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
		// TO DO
	}

	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
		if (null == rootPath || "".equals(rootPath.trim())) {
			return;
		}
		Set<String> mappers = PackageScanner.filterByAnnotation(rootPath.split(","), NameSpace.class);
		for (String mapper : mappers) {
			Class<?> clz = null;
			try {
				clz = Class.forName(mapper);
			} catch (ClassNotFoundException e) {
				logger.error(e.getMessage(), e);
				continue;
			}
			if (clz.isInterface()) {
				// 只代理接口
				BeanDefinitionBuilder rbf = BeanDefinitionBuilder.genericBeanDefinition();
				rbf.addPropertyValue("interfaceClz", clz);
				rbf.addPropertyValue("context", context);
				GenericBeanDefinition bdf = (GenericBeanDefinition) rbf.getBeanDefinition();
				bdf.setScope(ConfigurableBeanFactory.SCOPE_SINGLETON);
				bdf.setAutowireCandidate(true);
				bdf.setAutowireMode(AbstractBeanDefinition.AUTOWIRE_BY_TYPE);
				bdf.setBeanClass(MapperFactory.class);
				String beanName = "rabbit-orm-" + clz.getSimpleName();
				registry.registerBeanDefinition(beanName, bdf);
				logger.info("interface[{}] is registered to [{}]", clz.getSimpleName(), beanName);
			}
		}
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) {
		context = applicationContext;
	}
	
	public void setRootPath(String rootPath) {
		this.rootPath = rootPath;
	}

}
