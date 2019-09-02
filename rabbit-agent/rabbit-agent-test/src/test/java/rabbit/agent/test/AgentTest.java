package rabbit.agent.test;

import agent.rabbit.open.AgentDelegator;
import agent.rabbit.open.CallStackTracker;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RunWith(JUnit4.class)
public class AgentTest {

    private Logger logger = LoggerFactory.getLogger(AgentTest.class);

    @Test
    public void callStackTrackTest() {
        CallStackTracker handler = new CallStackTracker();
        AgentDelegator.addDelegationHandler(handler);
        new TestRunner().h1();
        logger.info("\n" + handler.getStacks());
    }
}
