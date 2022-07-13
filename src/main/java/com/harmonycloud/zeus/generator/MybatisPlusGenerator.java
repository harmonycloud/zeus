package com.harmonycloud.zeus.generator;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.baomidou.mybatisplus.generator.AutoGenerator;
import com.baomidou.mybatisplus.generator.InjectionConfig;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.FileOutConfig;
import com.baomidou.mybatisplus.generator.config.GlobalConfig;
import com.baomidou.mybatisplus.generator.config.PackageConfig;
import com.baomidou.mybatisplus.generator.config.StrategyConfig;
import com.baomidou.mybatisplus.generator.config.TemplateConfig;
import com.baomidou.mybatisplus.generator.config.po.TableInfo;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;

public class MybatisPlusGenerator {
    public static void main(String[] args) {
        Properties properties = new Properties();
        InputStream in = MybatisPlusGenerator.class.getClassLoader().getResourceAsStream("sql.code.generator.properties");
        try {
            properties.load(in);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // 代码生成器
        AutoGenerator mpg = new AutoGenerator();

        // 全局配置
        GlobalConfig gc = new GlobalConfig();
        String projectPath = System.getProperty("user.dir");
        System.out.println(projectPath);
        gc.setOutputDir(projectPath + "/src/main/java");
        gc.setAuthor(properties.getProperty("global.author"));
        gc.setOpen(Boolean.parseBoolean(properties.getProperty("global.output.open"))); // 是否打开输出目录,默认true
        gc.setEnableCache(Boolean.parseBoolean(properties.getProperty("global.cache.enable"))); // 是否在xml中添加二级缓存配置,默认false
        gc.setSwagger2(Boolean.parseBoolean(properties.getProperty("global.swagger.enable")));// 是否开启swagger2模式,默认false
        gc.setFileOverride(Boolean.parseBoolean(properties.getProperty("global.filed.override"))); // 是否覆盖已有文件
        gc.setEntityName("Bean%s");
        gc.setMapperName("Bean%sMapper");
        gc.setXmlName("Bean%sMapper");
        mpg.setGlobalConfig(gc);

        // 数据源配置
        DataSourceConfig dsc = new DataSourceConfig();
        dsc.setUrl(properties.getProperty("datasource.url"));
        dsc.setDriverName(properties.getProperty("datasource.driver"));
        dsc.setUsername(properties.getProperty("datasource.username"));
        dsc.setPassword(properties.getProperty("datasource.password"));
        mpg.setDataSource(dsc);

        // 包配置
        PackageConfig pc = new PackageConfig();
        pc.setModuleName(properties.getProperty("package.name"));
        pc.setParent(properties.getProperty("package.parent"));
        pc.setEntity("bean");
        pc.setMapper("dao");
        mpg.setPackageInfo(pc);

        // 自定义配置
        InjectionConfig cfg = new InjectionConfig() {
            @Override
            public void initMap() {
                // to do nothing
            }
        };

        List<FileOutConfig> focList = new ArrayList<>();
        focList.add(new FileOutConfig("/templates/mapper.xml.ftl") {
            @Override
            public String outputFile(TableInfo tableInfo) {
                return projectPath + "/src/main/resources/mapping/" + tableInfo.getEntityName() + "Mapper.xml";
            }
        });
        cfg.setFileOutConfigList(focList);

        mpg.setCfg(cfg);
        TemplateConfig templateConfig = new TemplateConfig();
        templateConfig.setXml(null);
        templateConfig.setController(null);
        templateConfig.setService(null);
        templateConfig.setServiceImpl(null);
        mpg.setTemplate(templateConfig);

        // 策略配置
        StrategyConfig strategy = new StrategyConfig();
        strategy.setNaming(NamingStrategy.underline_to_camel);
        strategy.setColumnNaming(NamingStrategy.underline_to_camel);
        strategy.setEntityLombokModel(true);
        strategy.setRestControllerStyle(false);
        // 当对某张表有所改动但只想重新生成这张表，可以这样设置
         strategy.setInclude("alert_setting");
        // 排除通用字段
        strategy.setSuperEntityColumns();
        strategy.setControllerMappingHyphenStyle(true);
        strategy.setEntityBooleanColumnRemoveIsPrefix(true);
        strategy.setEntityTableFieldAnnotationEnable(true);
        mpg.setStrategy(strategy);
        // 选择 freemarker 引擎需要指定如下加，注意 pom 依赖必须有！
        mpg.setTemplateEngine(new FreemarkerTemplateEngine());
        mpg.execute();
    }
}
