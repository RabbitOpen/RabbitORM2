package agent.rabbit.open;

import agent.rabbit.open.DelegationHandler;

import java.lang.reflect.Method;

/**
 * 调用链追踪
 */
public class CallStackTracker implements DelegationHandler<AgentContext> {

    // 记录当前线程入口方法
    private static ThreadLocal<AgentContext> rootContext = new ThreadLocal<>();

    // 记录当前拦截的方法
    private static ThreadLocal<AgentContext> currentContext = new ThreadLocal<>();

    private String stacks;

    @Override
    public AgentContext before(Method method, Object[] args) {
        AgentContext context = new AgentContext(method);
        AgentContext parent = currentContext.get();
        currentContext.set(context);
        context.setParent(parent);
        if (null == rootContext.get()) {
            rootContext.set(context);
        }
        return context;
    }

    @Override
    public void error(AgentContext context, Throwable t, Method method, Object[] args) {
        rootContext.remove();
        currentContext.remove();
    }

    @Override
    public void after(AgentContext context, Method method, Object[] args) {
        if (null == context) {
            return;
        }
        context.finish();
        if (context.equals(rootContext.get())) {
            // rootContext结束时清空 所有context；
            currentContext.remove();
            stacks = rootContext.get().getStacks();
            rootContext.remove();
        } else {
            currentContext.set(context.getParent());
        }
    }

    public String getStacks() {
        return stacks;
    }
}
