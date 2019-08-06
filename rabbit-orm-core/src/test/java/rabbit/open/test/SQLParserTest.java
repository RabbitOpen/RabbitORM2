package rabbit.open.test;

import junit.framework.TestCase;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import rabbit.open.common.exception.WrongMappingFilePathException;
import rabbit.open.orm.dml.name.SQLParser;

@RunWith(JUnit4.class)
public class SQLParserTest {

    @Test
    public void wrongPathTest() {
        try {
            new SQLParser("user").doXmlParsing();
        } catch (Exception e) {
            TestCase.assertSame(e.getClass(), WrongMappingFilePathException.class);
        }
    }

}
