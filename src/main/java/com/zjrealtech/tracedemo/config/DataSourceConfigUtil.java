package com.zjrealtech.tracedemo.config;

import javax.sql.DataSource;

class DataSourceConfigUtil {
    /**
     * 更新DataSource配置源的url
     * @param dataSource tomcat连接池的datasource，目前使用的是tomcat连接池
     * @return 添加了sendStringParametersAsUnicode=false的DataSource
     */
    static org.apache.tomcat.jdbc.pool.DataSource updateDataSourceUrl(DataSource dataSource){
        org.apache.tomcat.jdbc.pool.DataSource tomcatDataSource = (org.apache.tomcat.jdbc.pool.DataSource)dataSource;
        //设置sendStringParametersAsUnicode=false避免影响性能
        String url = tomcatDataSource.getUrl() + ";sendStringParametersAsUnicode=false";
        tomcatDataSource.setUrl(url);
        return tomcatDataSource;
    }
}
