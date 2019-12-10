package rabbit.open.dtx.common.spring.enhance.ext;

import org.aopalliance.intercept.MethodInvocation;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import rabbit.open.dtx.common.context.DistributedTransactionContext;
import rabbit.open.dtx.common.nio.client.DistributedTransactionManger;
import rabbit.open.dtx.common.nio.client.DtxClient;
import rabbit.open.dtx.common.nio.client.FutureResult;
import rabbit.open.dtx.common.nio.client.ext.DtxResourcePool;
import rabbit.open.dtx.common.nio.exception.NetworkException;
import rabbit.open.dtx.common.nio.exception.RpcException;
import rabbit.open.dtx.common.nio.exception.TimeoutException;
import rabbit.open.dtx.common.nio.exception.TransactionManagerNotFoundException;
import rabbit.open.dtx.common.nio.pub.protocol.RabbitProtocol;
import rabbit.open.dtx.common.spring.anno.DtxService;
import rabbit.open.dtx.common.spring.anno.Namespace;
import rabbit.open.dtx.common.spring.anno.Reference;
import rabbit.open.dtx.common.spring.enhance.AbstractAnnotationEnhancer;
import rabbit.open.dtx.common.spring.enhance.PointCutHandler;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RPC 调用增强
 * @author xiaoqianbin
 * @date 2019/12/7
 **/
public class DtxServiceScanner extends AbstractAnnotationEnhancer<Reference> implements ApplicationContextAware {

    private transient ApplicationContext context;

    // key: namespace, value: dtxService object
    private static final Map<String, Object> dtxServiceCache = new ConcurrentHashMap<>();

    /**
     * <b>@Reference 注解对应的class和它的namespace的缓存</b>
     * key: reference class, value: namespace
     **/
    private static final Map<Class<?>, String> namespaceCache = new ConcurrentHashMap<>();

    /**
     * 不同资源池的缓存
     * @author xiaoqianbin
     * @date 2019/12/9
     **/
    private static final Map<String, DtxResourcePool> poolCache = new ConcurrentHashMap<>();

    @Override
    protected PointCutHandler<Reference> getHandler() {
        return (invocation, annotation) -> {
            checkAndRegisterResourcePool(annotation);
            if ("equals".equals(invocation.getMethod().getName())) {
                return false;
            }
            if ("hashCode".equals(invocation.getMethod().getName())) {
                return -1;
            }
            if ("toString".equals(invocation.getMethod().getName())) {
                return invocation.getThis().getClass().getName();
            }
            return doInvoke(invocation, annotation);
        };
    }

    // rpc 调用
    private Object doInvoke(MethodInvocation invocation, Reference annotation) {
        Object data;
        DtxClient dtxClient = null;
        try {
            Method method = invocation.getMethod();
            String namespace = namespaceCache.get(invocation.getThis().getClass());
            RabbitProtocol protocol = new RabbitProtocol(namespace, method.getName(), method.getParameterTypes(), invocation.getArguments());
            dtxClient = poolCache.get(annotation.transactionManager()).getResource(50);
            FutureResult result = dtxClient.send(protocol);
            dtxClient.release();
            Long timeout = DistributedTransactionContext.getRollbackTimeout();
            data = result.getData(null == timeout ? annotation.timeoutSeconds() : timeout);
        } catch (TimeoutException e) {
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RpcException(e);
        } catch (NetworkException e) {
            if (null != dtxClient) {
                dtxClient.destroy();
            }
            throw e;
        }
        if (data instanceof Exception) {
            throw new RpcException((Exception)data);
        }
        return data;
    }

    /**
     * 检查注册信息，有必要的话就注册一个资源池
     * @param    annotation
     * @author xiaoqianbin
     * @date 2019/12/9
     **/
    private void checkAndRegisterResourcePool(Reference annotation) {
        DistributedTransactionManger manger = (DistributedTransactionManger) context.getBean(annotation.transactionManager());
        if (null == manger) {
            throw new TransactionManagerNotFoundException(annotation.transactionManager());
        }
        if (!poolCache.containsKey(annotation.transactionManager())) {
            synchronized (DtxServiceScanner.class) {
                if (poolCache.containsKey(annotation.transactionManager())) {
                    return;
                }
                try {
                    DtxResourcePool dtxResourcePool = new DtxResourcePool(manger);
                    poolCache.put(annotation.transactionManager(), dtxResourcePool);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 增强有@Reference注解的类
     * @param    beanClass
     * @param    beanName
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) {
        if (null != beanClass.getAnnotation(getTargetAnnotation())) {
            if (1 != beanClass.getInterfaces().length) {
                // 声明了Reference的class不能实现多个接口
                throw new BeanCreationException(String.format("too many interface were implemented by %s", beanClass));
            }
            Namespace namespace = beanClass.getInterfaces()[0].getAnnotation(Namespace.class);
            if (null == namespace) {
                throw new BeanCreationException(String.format("@%s can't be set on %s", Reference.class.getSimpleName(), beanClass));
            }
            namespaceCache.put(beanClass, namespace.value());
            beans2Enhance.add(beanName);
        }
        return super.postProcessBeforeInstantiation(beanClass, beanName);
    }

    /**
     * 缓存bean Service
     * @param    bean
     * @param    beanName
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        DtxService dtxService = bean.getClass().getAnnotation(DtxService.class);
        if (null != dtxService) {
            // 扫描dtxService实现
            for (Class<?> anInterface : bean.getClass().getInterfaces()) {
                Namespace namespace = anInterface.getAnnotation(Namespace.class);
                if (null != namespace) {
                    assertDuplicatedNamespace(namespace.value());
                    dtxServiceCache.put(namespace.value(), bean);
                } else {
                    throw new BeanCreationException("@" + Namespace.class.getName() + " can't be empty");
                }
            }
        }
        return super.postProcessBeforeInitialization(bean, beanName);
    }

    @Override
    protected synchronized Object[] getPointCuts() {
        if (null != pointCuts) {
            return pointCuts;
        }
        pointCuts = new Object[]{new ReferencePointCut<>(getTargetAnnotation(), getHandler())};
        return pointCuts;
    }

    /**
     * <b>@Reference类型的bean全部使用cglib代理</b>
     * @param	beanClass
	 * @param	beanName
     * @author  xiaoqianbin
     * @date    2019/12/9
     **/
    @Override
    protected boolean shouldProxyTargetClass(Class<?> beanClass, String beanName) {
        if (!beans2Enhance.contains(beanName)) {
            return super.shouldProxyTargetClass(beanClass, beanName);
        }
        return true;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.context = applicationContext;
    }

    /**
     * 断言重复的命名空间
     * @param    namespace
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    private void assertDuplicatedNamespace(String namespace) {
        if (dtxServiceCache.containsKey(namespace)) {
            throw new BeanCreationException(String.format("duplicated dtxService namespace[%s] is found!",
                    namespace));
        }
    }

    /**
     * 根据命名空间查找对应的serviceBean
     * @param    namespace
     * @author xiaoqianbin
     * @date 2019/12/7
     **/
    public static final Object getDtxService(String namespace) {
        return dtxServiceCache.get(namespace);
    }

    @PreDestroy
    public void destroy() {
        for (Map.Entry<String, DtxResourcePool> entry : poolCache.entrySet()) {
            entry.getValue().gracefullyShutdown();
        }
    }
}
