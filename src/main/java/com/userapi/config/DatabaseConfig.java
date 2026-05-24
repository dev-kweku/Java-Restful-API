package com.userapi.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.github.cdimascio.dotenv.Dotenv;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {

    private static final Dotenv dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();

    private static HikariDataSource dataSource;

    public static synchronized DataSource getDataSource() {
        if (dataSource == null) {
            var config = new HikariConfig();


            config.setJdbcUrl (require("DB_URL"));
            config.setUsername(require("DB_USER"));
            config.setPassword(require("DB_PASS"));


            config.setMaximumPoolSize(20);
            config.setMinimumIdle(5);
            config.setConnectionTimeout(30_000);
            config.setIdleTimeout(600_000);
            config.setMaxLifetime(1_800_000);
            config.setPoolName("UserAPI-Pool");


            config.addDataSourceProperty("cachePrepStmts",        "true");
            config.addDataSourceProperty("prepStmtCacheSize",     "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            dataSource = new HikariDataSource(config);

            System.out.println("DB pool ready — " + config.getJdbcUrl()
                    + " | user=" + config.getUsername()
                    + " | poolSize=" + config.getMaximumPoolSize());
            System.out.flush();
        }
        return dataSource;
    }


    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }


    public static String get(String key) {
        return dotenv.get(key, System.getenv().getOrDefault(key, null));
    }

    public static String get(String key, String defaultValue) {
        String val = get(key);
        return (val != null && !val.isBlank()) ? val : defaultValue;
    }

    private static String require(String key) {
        String val = get(key);
        if (val == null || val.isBlank()) {
            throw new IllegalStateException(
                    "Missing required config: '" + key + "'. "
                            + "Add it to your .env file or set it as an environment variable."
            );
        }
        return val;
    }

    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("DB pool closed");
        }
    }
}