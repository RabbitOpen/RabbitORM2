package sqlite.test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SQLiteTest {

    @Test
    public void sqliteTest() throws ClassNotFoundException, SQLException {

        Class.forName("org.sqlite.JDBC");
        Properties pro = new Properties();
        pro.put("date_string_format", "yyyy-MM-dd HH:mm:ss");
        Connection conn = DriverManager.getConnection("jdbc:sqlite::resource:db/app.s3db", pro);
        
        ResultSet rs = conn.createStatement().executeQuery("select birth_day from t_user");
        while(rs.next()) {
            System.out.println(rs.getString(1));
            System.out.println(rs.getTimestamp(1));
        }
        
        conn.close();
        
    }
}
