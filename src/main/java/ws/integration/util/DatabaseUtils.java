package ws.integration.util;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DatabaseUtils {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseUtils.class);

    private DatabaseUtils() {
        throw new IllegalStateException("Utility class");
    }

    public static void close(Connection conn) {
        if (conn != null) {
            try {
                if (!conn.isClosed()) {
                    conn.close();
                }
            } catch (SQLException ex) {
                logger.warn("Failed to close Connection: {}", ex.getMessage());
            }
        }
    }

    public static void close(Statement st) {
        if (st != null) {
            try {
                st.close();
            } catch (SQLException ex) {
                logger.warn("Failed to close Statement: {}", ex.getMessage());
            }
        }
    }

    public static void close(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException ex) {
                logger.warn("Failed to close ResultSet: {}", ex.getMessage());
            }
        }
    }

    public static void close(Statement st, ResultSet rs) {
        close(rs);
        close(st);
    }

    public static void close(Connection conn, Statement st, ResultSet rs) {
        close(rs);
        close(st);
        close(conn);
    }

    public static java.sql.Timestamp getCurrentTimestamp() {
        return new java.sql.Timestamp(new java.util.Date().getTime());
    }
}
