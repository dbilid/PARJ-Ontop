/**
 * Copyright MaDgIK Group 2010 - 2012.
 */
package madgik.exareme.utils.embedded.db;


import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author herald
 */
public class SQLite {

  private static Logger log = LoggerFactory.getLogger(SQLite.class);

  private SQLite() {
  }

  public static Connection createConnection(String database) throws SQLException {
    try {
      Class.forName("org.sqlite.JDBC");
    } catch (Exception e) {
      throw new SQLException("SQLite driver not found", e);
    }
    Connection conn = DriverManager.getConnection("jdbc:sqlite:" + database);
    return conn;
  }
}
