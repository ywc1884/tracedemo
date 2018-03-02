package com.zjrealtech.tracedemo.config;

import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

class DataSourceConfigUtil {
    /**
     * 更新DataSource配置源的url
     * @param dataSource tomcat连接池的datasource，目前使用的是tomcat连接池
     * @return 添加了sendStringParametersAsUnicode=false的DataSource
     */
    static HikariDataSource updateDataSourceUrl(DataSource dataSource){
        HikariDataSource hikariDataSource = (HikariDataSource)dataSource;
        //设置sendStringParametersAsUnicode=false避免影响性能
        String url = hikariDataSource.getJdbcUrl() + ";sendStringParametersAsUnicode=false";
        hikariDataSource.setJdbcUrl(url);
        return hikariDataSource;
    }
}
