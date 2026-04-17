package com.trainerclienthub.db;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;
import java.util.logging.Logger;

// Manages the single shared JDBC connection for the Trainer-Client Hub application.

public class DatabaseConnection {

    // Logger

    private static final Logger LOGGER = Logger.getLogger(DatabaseConnection.class.getName());

    // Configuration keys

    private static final String PROPERTIES_FILE = "db.properties";
    private static final String KEY_DRIVER = "db.driver";
    private static final String KEY_URL = "db.url";
    private static final String KEY_USERNAME = "db.username";
    private static final String KEY_PASSWORD = "db.password";
    private static final String KEY_LOGIN_TIMEOUT = "db.loginTimeout";
    private static final int DEFAULT_TIMEOUT = 10; // seconds

    // Singleton state

    private static volatile DatabaseConnection instance;

    private final Properties config;

    // Private constructor

    private DatabaseConnection() {
        this.config = loadProperties();
        registerDriver();
        LOGGER.info("DatabaseConnection: configuration loaded successfully.");
    }

    // Singleton accessor

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    // Public API

    public Connection getConnection() {
        return openConnection();
    }

    public void close() {
        LOGGER.info("DatabaseConnection: close() called (no persistent connection to release).");
    }

    public boolean isConnected() {
        try (Connection test = openConnection()) {
            return test != null && !test.isClosed();
        } catch (Exception e) {
            return false;
        }
    }

    // Private helpers

    private Properties loadProperties() {
        Properties props = new Properties();

        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {

            if (stream == null) {
                throw new DatabaseException(
                        "Configuration file \"" + PROPERTIES_FILE + "\" not found on classpath. "
                                + "Ensure it exists at src/main/resources/" + PROPERTIES_FILE);
            }
            props.load(stream);
            LOGGER.fine("DatabaseConnection: loaded configuration from " + PROPERTIES_FILE);

        } catch (IOException e) {
            throw new DatabaseException(
                    "Failed to read database configuration from \"" + PROPERTIES_FILE + "\".", e);
        }

        // Validate required properties
        validateProperty(props, KEY_URL, "db.url");
        validateProperty(props, KEY_USERNAME, "db.username");
        validateProperty(props, KEY_PASSWORD, "db.password");

        return props;
    }

    private void registerDriver() {
        String driverClass = config.getProperty(KEY_DRIVER, "com.mysql.cj.jdbc.Driver");
        try {
            Class.forName(driverClass);
            LOGGER.fine("DatabaseConnection: driver registered — " + driverClass);
        } catch (ClassNotFoundException e) {
            throw new DatabaseException(
                    "MySQL JDBC driver not found: \"" + driverClass + "\". "
                            + "Add mysql-connector-j to your project dependencies (pom.xml).",
                    e);
        }
    }

    private Connection openConnection() {
        String url = config.getProperty(KEY_URL);
        String username = config.getProperty(KEY_USERNAME);
        String password = config.getProperty(KEY_PASSWORD);

        int loginTimeout = DEFAULT_TIMEOUT;
        try {
            loginTimeout = Integer.parseInt(
                    config.getProperty(KEY_LOGIN_TIMEOUT, String.valueOf(DEFAULT_TIMEOUT)));
        } catch (NumberFormatException e) {
            LOGGER.warning("DatabaseConnection: invalid db.loginTimeout value — "
                    + "using default of " + DEFAULT_TIMEOUT + "s.");
        }

        DriverManager.setLoginTimeout(loginTimeout);

        try {
            Connection conn = DriverManager.getConnection(url, username, password);
    
            conn.setAutoCommit(true);
            return conn;

        } catch (SQLException e) {
            throw new DatabaseException(
                    "Unable to connect to the database. "
                            + "Please verify the URL, credentials, and that MySQL is running.\n"
                            + "URL attempted: " + url + "\n"
                            + "SQL state: " + e.getSQLState()
                            + " | Error code: " + e.getErrorCode(),
                    e);
        }
    }

    private void validateProperty(Properties props, String key, String friendlyName) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new DatabaseException(
                    "Required database property \"" + friendlyName + "\" is missing or blank "
                            + "in " + PROPERTIES_FILE + ".");
        }
    }
}