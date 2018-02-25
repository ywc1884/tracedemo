package com.zjrealtech.tracedemo.config;

import org.apache.ibatis.session.SqlSessionFactory;
import org.mybatis.spring.SqlSessionFactoryBean;
import org.mybatis.spring.SqlSessionTemplate;
import org.mybatis.spring.annotation.MapperScan;
import org.mybatis.spring.boot.autoconfigure.SpringBootVFS;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.stereotype.Component;
import javax.sql.DataSource;

@Component
@Configuration
@MapperScan(basePackages = "com.zjrealtech.tracedemo.mapper", sqlSessionTemplateRef = "biSqlSessionTemplate")
class DataSourceBiConfig {
    @Value("${mybatis.type-aliases-package}")
    private
    String typeAliasesPackage;

    @Bean(name = "biDataSource")
    @ConfigurationProperties("bi.datasource")
    @Primary
    public DataSource testDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean(name = "biSqlSessionFactory")
    @Primary
    public SqlSessionFactory testSqlSessionFactory(@Qualifier("biDataSource") DataSource dataSource) throws Exception {
        SqlSessionFactoryBean bean = new SqlSessionFactoryBean();
        bean.setDataSource(DataSourceConfigUtil.updateDataSourceUrl(dataSource));
        bean.setMapperLocations(new PathMatchingResourcePatternResolver().getResources("classpath*:/mapper/*.xml"));
        bean.setTypeAliasesPackage(typeAliasesPackage);
        bean.setVfs(SpringBootVFS.class);
        return bean.getObject();
    }

    @Bean(name = "biTransactionManager")
    @Primary
    public DataSourceTransactionManager testTransactionManager(@Qualifier("biDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(DataSourceConfigUtil.updateDataSourceUrl(dataSource));
    }

    @Bean(name = "biSqlSessionTemplate")
    @Primary
    public SqlSessionTemplate testSqlSessionTemplate(@Qualifier("biSqlSessionFactory") SqlSessionFactory sqlSessionFactory) {
        return new SqlSessionTemplate(sqlSessionFactory);
    }
}
