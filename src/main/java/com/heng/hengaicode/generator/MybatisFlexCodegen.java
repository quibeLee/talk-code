package com.heng.hengaicode.generator;

import cn.hutool.core.lang.Dict;
import cn.hutool.setting.yaml.YamlUtil;
import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Map;

/**
 * MybatisFlex代码生成器
 */
public class MybatisFlexCodegen {

    public static final String[] TABLE_NAMES = {"chat_history"};

    public static void main(String[] args) {
        Dict dict = YamlUtil.loadByPath("application.yaml");
        Map<String, Object> datasource = dict.getByPath("spring.datasource");
        String jdbcUrl = datasource.get("url").toString();
        String username = datasource.get("username").toString();
        String password = datasource.get("password").toString();
        //配置数据源
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(jdbcUrl);
        dataSource.setUsername(username);
        dataSource.setPassword(password);

        //创建配置内容
        GlobalConfig globalConfig = createGlobalConfig();

        //通过 datasource 和 globalConfig 创建代码生成器
        Generator generator = new Generator(dataSource, globalConfig);
        //生成代码
        generator.generate();
    }

    public static GlobalConfig createGlobalConfig() {
        //创建配置内容
        GlobalConfig globalConfig = new GlobalConfig();

        //设置根包
        globalConfig.getPackageConfig()
                .setBasePackage("com.heng.hengaicode.gen");

        //设置生成哪些表，setGenerateTable 未配置时，生成所有表
        globalConfig.getStrategyConfig()
                .setGenerateTable(TABLE_NAMES)
                .setLogicDeleteColumn("isDelete");

        //设置生成 entity 并启用 Lombok
        globalConfig.enableEntity()
                .setWithLombok(true)
                .setJdkVersion(21);
        //设置生成 mapper
        globalConfig.enableMapper();
        globalConfig.enableMapperXml();

        //生成service
        globalConfig.enableService();
        globalConfig.enableServiceImpl();

        //生成controller
        globalConfig.enableController();

        globalConfig.getJavadocConfig()
                .setAuthor("heng-ai-code")
                .setSince("");

        return globalConfig;
    }
}