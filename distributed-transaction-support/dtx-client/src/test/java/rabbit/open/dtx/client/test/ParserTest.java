package rabbit.open.dtx.client.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.client.datasource.parser.SQLStructure;
import rabbit.open.dtx.client.datasource.parser.SQLType;
import rabbit.open.dtx.client.datasource.parser.SimpleSQLParser;

/**
 * @author xiaoqianbin
 * @date 2019/12/3
 **/
@RunWith(JUnit4.class)
public class ParserTest {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Test
    public void updateParserTest() {
        SQLStructure structure = SimpleSQLParser.parse(" update \r user set user.name = 'zhans', user.age = ?, user.sex = ? where user.id = 1");
        TestCase.assertEquals(structure.getColumns().get(0).getColumnName(), "user.name");
        TestCase.assertEquals(structure.getColumns().get(0).getValue(), "'zhans'");
        TestCase.assertEquals(structure.getColumns().get(1).getColumnName(), "user.age");
        TestCase.assertEquals(structure.getColumns().get(1).getValue(), "?");
        TestCase.assertEquals(structure.getColumns().get(1).getPlaceHolderIndex(), 0);
        TestCase.assertEquals(structure.getColumns().get(2).getColumnName(), "user.sex");
        TestCase.assertEquals(structure.getColumns().get(2).getValue(), "?");
        TestCase.assertEquals(structure.getColumns().get(2).getPlaceHolderIndex(), 1);
        TestCase.assertEquals(structure.getTargetTables(), "user");
        TestCase.assertEquals(structure.getSqlType(), SQLType.UPDATE);
        TestCase.assertEquals(structure.getCondition(), "where user.id = 1");
        logger.info("update --> {}", structure.getFormattedSql());
    }

    @Test
    public void deleteParserTest() {
        SQLStructure structure = SimpleSQLParser.parse(" delete \n\r from user where user.id > 1");
        TestCase.assertEquals(structure.getTargetTables(), "user");
        TestCase.assertEquals(structure.getSqlType(), SQLType.DELETE);
        TestCase.assertEquals(structure.getCondition(), "where user.id > 1");
        logger.info("delete --> {}", structure.getFormattedSql());
    }

    @Test
    public void insertParserTest() {
        SQLStructure structure = SimpleSQLParser.parse(" insert user(id, name) values\n(1, 'hello')");
        TestCase.assertEquals(structure.getTargetTables(), "user");
        TestCase.assertEquals(structure.getColumns().get(0).getColumnName(), "id");
        TestCase.assertEquals(structure.getColumns().get(1).getColumnName(), "name");
        TestCase.assertEquals(structure.getSqlType(), SQLType.INSERT);
        logger.info("insert --> {}", structure.getFormattedSql());

        structure = SimpleSQLParser.parse(" insert into user(ids, name, age, home) values\n(1, 'hello', ?, ?)");
        TestCase.assertEquals(structure.getTargetTables(), "user");
        TestCase.assertEquals(structure.getColumns().get(0).getColumnName(), "ids");
        TestCase.assertEquals(structure.getColumns().get(1).getColumnName(), "name");
        TestCase.assertEquals(structure.getColumns().get(2).getColumnName(), "age");
        TestCase.assertEquals(structure.getColumns().get(2).getValue(), "?");
        TestCase.assertEquals(structure.getColumns().get(2).getPlaceHolderIndex(), 0);
        TestCase.assertEquals(structure.getColumns().get(3).getColumnName(), "home");
        TestCase.assertEquals(structure.getColumns().get(3).getValue(), "?");
        TestCase.assertEquals(structure.getColumns().get(3).getPlaceHolderIndex(), 1);
        TestCase.assertEquals(structure.getSqlType(), SQLType.INSERT);
        logger.info("insert --> {}", structure.getFormattedSql());
        TestCase.assertTrue(structure == SimpleSQLParser.parse(" insert into user(ids, name, age, home) values\n(1, 'hello', ?, ?)"));
    }
}
