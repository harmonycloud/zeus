package com.harmonycloud.zeus.config;

import com.harmonycloud.zeus.service.middleware.EsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * @author liyinlong
 * @since 2022/8/12 4:11 下午
 */
@Slf4j
@Component
public class InitElasticsearchTemplate {

    @Autowired
    private EsService esService;

    @PostConstruct
    public void init() throws Exception {
        //创建es模板
        try {
            esService.initEsIndexTemplate();
            log.info("集群:{}索引模板初始化成功");
        } catch (Exception e) {
            log.error("集群:{}索引模板初始化失败", e);
        }
    }

}