package com.sambath.admincafe.common;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class Building {

    public enum Env {
        Local,
        Dev
    }

    public static final Env mode = Env.Dev;

    @Bean
    public DataSource getDataSource() {
        DataSourceBuilder<?> builder = DataSourceBuilder.create()
                .driverClassName("org.postgresql.Driver");

        switch (mode) {
            case Local -> builder
                    .url("jdbc:postgresql://192.168.178.128:5432/wehr")
                    .username("wehr")
                    .password("wehr");
            case Dev -> builder
                    .url("jdbc:postgresql://dpg-d8f8i4rbc2fs73effd4g-a:5432/leng_sambath")
                    .username("leng_sambath_user")
                    .password("7yA3J3dW8B6AOyWugyKd1pB4A00bYLge");
        }

        return builder.build();
    }
}
