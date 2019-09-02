package agent.rabbit.open;

import agent.net.bytebuddy.agent.builder.AgentBuilder;
import agent.net.bytebuddy.description.NamedElement;
import agent.net.bytebuddy.description.method.MethodDescription;
import agent.net.bytebuddy.description.type.TypeDescription;
import agent.net.bytebuddy.dynamic.DynamicType;
import agent.net.bytebuddy.implementation.MethodDelegation;
import agent.net.bytebuddy.matcher.ElementMatcher;
import agent.net.bytebuddy.matcher.ElementMatchers;
import agent.net.bytebuddy.utility.JavaModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.instrument.Instrumentation;

/**
 * 针对rabbit orm开发的agent
 */
public class RabbitAgentEntry {

	private static Logger logger = LoggerFactory.getLogger(RabbitAgentEntry.class);

	public static void premain(String agentArgs, Instrumentation inst) {
		AgentBuilder.Transformer transformer = (builder, typeDescription, classLoader, javaModule) -> builder
				.method(ElementMatchers.<MethodDescription>any()) // 拦截任意方法
				.intercept(MethodDelegation.to(AgentDelegator.class)); // 委托
		String[] packages = null;
		if (null == agentArgs) {
			packages = new String[] { "rabbit.open.orm" };
		} else {
			packages = agentArgs.split(",");
		}
		logger.info("RabbitOrmAgent is running....");
		logger.info("scan target package [{}]", packages[0]);
		ElementMatcher.Junction<NamedElement> matcher = ElementMatchers.nameStartsWith(packages[0]);
		for (int i = 1; i < packages.length; i++) {
			matcher.or(ElementMatchers.nameStartsWith(packages[i]));
			logger.info("scan target package [{}]", packages[i]);
		}
		new AgentBuilder.Default().type(matcher) // 指定需要拦截的类
				.transform(transformer).with(getEmptyListener()).installOn(inst);
	}

	private static AgentBuilder.Listener getEmptyListener() {
		return new AgentBuilder.Listener() {
			@Override
			public void onDiscovery(String s, ClassLoader classLoader, JavaModule javaModule, boolean b) {
				// TO DO
			}

			@Override
			public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader,
					JavaModule javaModule, boolean b, DynamicType dynamicType) {
				// TO DO
			}

			@Override
			public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule javaModule,
					boolean b) {
				// TO DO
			}

			@Override
			public void onError(String s, ClassLoader classLoader, JavaModule javaModule, boolean b,
					Throwable throwable) {
				// TO DO
			}

			@Override
			public void onComplete(String s, ClassLoader classLoader, JavaModule javaModule, boolean b) {
				// TO DO
			}

		};
	}

}
