package ws.integration.config;

/**
 * Application configuration singleton.
 * All values are read from environment variables at startup, with localhost
 * defaults to allow running without any configuration.
 *
 * Environment variables:
 *   DB_URL       – full JDBC URL  (default: jdbc:postgresql://localhost:5432/customer)
 *   DB_USER      – database username (default: postgres)
 *   DB_PASSWORD  – database password (default: empty)
 *   CUS20_DOMAIN – base URL of the CUS-20 customer service (default: http://localhost:3001)
 */
public final class AppConfig {

    private static AppConfig instance;

    private final String dbUrl;
    private final String dbUsername;
    private final String dbPassword;
    private final String cus20Domain;
    private final String searchCustomerUrl;

    private AppConfig() {
        this.dbUrl           = env("DB_URL",       "jdbc:postgresql://localhost:5432/customer");
        this.dbUsername      = env("DB_USER",      "postgres");
        this.dbPassword      = env("DB_PASSWORD",  "");
        this.cus20Domain     = env("CUS20_DOMAIN", "http://localhost:3001");
        this.searchCustomerUrl = this.cus20Domain + "/custom/search";
    }

    private static String env(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) {
            instance = new AppConfig();
        }
        return instance;
    }

    public String getDbUrl()             { return dbUrl; }
    public String getDbUsername()        { return dbUsername; }
    public String getDbPassword()        { return dbPassword; }
    public String getCus20Domain()       { return cus20Domain; }
    public String getSearchCustomerUrl() { return searchCustomerUrl; }
}
