package com.userapi.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

public class DatabaseConfig {
    private static HikariDataSource dataSource;

    public static DataSource getDataSource(){
        if(dataSource == null){
            var config=new HikariConfig();
            config.setJdbcUrl(System.getenv().getOrDefault(
                    "DB_URL","jdbc:postgresql://localhost/degraft"
            ));
            config.setUsername(System.getenv().getOrDefault("DB_USER","degraft"));
        }
        return dataSource;
    }
}
