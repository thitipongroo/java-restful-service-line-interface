package ws.integration.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.integration.config.AppConfig;

public final class ConnectionDB {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionDB.class);

    private ConnectionDB() {
        throw new IllegalStateException("Utility class");
    }

    public static Connection getDbCustomer() throws SQLException {
        AppConfig cfg = AppConfig.getInstance();
        return DriverManager.getConnection(cfg.getDbUrl(), cfg.getDbUsername(), cfg.getDbPassword());
    }
}
