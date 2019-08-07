package rabbit.open.test;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import rabbit.open.orm.common.exception.WrongMappingFilePathException;
import rabbit.open.orm.core.dml.name.SQLParser;

@RunWith(JUnit4.class)
public class SQLParserTest {

    @Test
    public void wrongPathTest() {
        try {
            new SQLParser("user").doXmlParsing();
            throw new RuntimeException("wrongPathTest error");
        } catch (Exception e) {
            TestCase.assertSame(e.getClass(), WrongMappingFilePathException.class);
        }
    }

}
