package rabbit.open.dtx.client.test;

import junit.framework.TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rabbit.open.dtx.client.datasource.parser.SQLMeta;
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
        SQLMeta sqlMeta = SimpleSQLParser.parse(" update \r user set user.name = 'zhans', user.age = ?, user.sex = ? where user.id = 1");
        TestCase.assertEquals(sqlMeta.getColumns().get(0).getColumnName(), "user.name");
        TestCase.assertEquals(sqlMeta.getColumns().get(0).getValue(), "'zhans'");
        TestCase.assertEquals(sqlMeta.getColumns().get(1).getColumnName(), "user.age");
        TestCase.assertEquals(sqlMeta.getColumns().get(1).getValue(), "?");
        TestCase.assertEquals(sqlMeta.getColumns().get(1).getPlaceHolderIndex(), 0);
        TestCase.assertEquals(sqlMeta.getColumns().get(2).getColumnName(), "user.sex");
        TestCase.assertEquals(sqlMeta.getColumns().get(2).getValue(), "?");
        TestCase.assertEquals(sqlMeta.getColumns().get(2).getPlaceHolderIndex(), 1);
        TestCase.assertEquals(sqlMeta.getTargetTables(), "user");
        TestCase.assertEquals(sqlMeta.getSqlType(), SQLType.UPDATE);
        TestCase.assertEquals(sqlMeta.getCondition(), "where user.id = 1");
        logger.info("update --> {}", sqlMeta.getFormattedSql());
    }

    @Test
    public void deleteParserTest() {
        SQLMeta sqlMeta = SimpleSQLParser.parse(" delete \n\r from user where user.id > 1");
        TestCase.assertEquals(sqlMeta.getTargetTables(), "user");
        TestCase.assertEquals(sqlMeta.getSqlType(), SQLType.DELETE);
        TestCase.assertEquals(sqlMeta.getCondition(), "where user.id > 1");
        logger.info("delete --> {}", sqlMeta.getFormattedSql());
    }

    @Test
    public void insertParserTest() {
        SQLMeta sqlMeta = SimpleSQLParser.parse(" insert user(id, name) values\n(1, 'hello')");
        TestCase.assertEquals(sqlMeta.getTargetTables(), "user");
        TestCase.assertEquals(sqlMeta.getColumns().get(0).getColumnName(), "id");
        TestCase.assertEquals(sqlMeta.getColumns().get(1).getColumnName(), "name");
        TestCase.assertEquals(sqlMeta.getSqlType(), SQLType.INSERT);
        logger.info("insert --> {}", sqlMeta.getFormattedSql());

        sqlMeta = SimpleSQLParser.parse(" insert into user(ids, name, age, home) values\n(1, 'hello', ?, ?)");
        TestCase.assertEquals(sqlMeta.getTargetTables(), "user");
        TestCase.assertEquals(sqlMeta.getColumns().get(0).getColumnName(), "ids");
        TestCase.assertEquals(sqlMeta.getColumns().get(0).getValue(), "1");
        TestCase.assertEquals(sqlMeta.getColumns().get(1).getColumnName(), "name");
        TestCase.assertEquals(sqlMeta.getColumns().get(2).getColumnName(), "age");
        TestCase.assertEquals(sqlMeta.getColumns().get(2).getValue(), "?");
        TestCase.assertEquals(sqlMeta.getColumns().get(2).getPlaceHolderIndex(), 0);
        TestCase.assertEquals(sqlMeta.getColumns().get(3).getColumnName(), "home");
        TestCase.assertEquals(sqlMeta.getColumns().get(3).getValue(), "?");
        TestCase.assertEquals(sqlMeta.getColumns().get(3).getPlaceHolderIndex(), 1);
        TestCase.assertEquals(sqlMeta.getSqlType(), SQLType.INSERT);
        logger.info("insert --> {}", sqlMeta.getFormattedSql());
        TestCase.assertTrue(sqlMeta == SimpleSQLParser.parse(" insert into user(ids, name, age, home) values\n(1, 'hello', ?, ?)"));
    }
}
