package com.atguigu.gmall.sms.config;

import com.zaxxer.hikari.HikariDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class DataSourceConfig {

//    @Bean
//    @ConfigurationProperties(prefix = "spring.datasource")
//    public DataSource hikariDataSource(@Value("${spring.datasource.url}")String url ){
//        HikariDataSource hikariDataSource = new HikariDataSource();
//        hikariDataSource.setJdbcUrl(url);
//        return hikariDataSource;
//    }

    @Bean
    @Primary
    public DataSource dataSourceProxy(
            @Value("${spring.datasource.driver-class-name}")String driverClassName,
            @Value("${spring.datasource.url}")String url ,
            @Value("${spring.datasource.username}")String userName ,
            @Value("${spring.datasource.password}")String passWord ){
        HikariDataSource hikariDataSource = new HikariDataSource();
        hikariDataSource.setDriverClassName(driverClassName);
        hikariDataSource.setJdbcUrl(url);
        hikariDataSource.setUsername(userName);
        hikariDataSource.setPassword(passWord);
        return new DataSourceProxy(hikariDataSource);
    }
}
