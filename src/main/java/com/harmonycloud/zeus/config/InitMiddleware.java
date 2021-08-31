package com.harmonycloud.zeus.config;

import java.io.File;

import javax.annotation.PostConstruct;

import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2021/8/19 11:35 上午
 */
@Slf4j
@Component
public class InitMiddleware {


    @Value("${k8s.component.helm:/usr/local/zeus-pv/helm}")
    private String helmPath;
    @Value("${k8s.component.middleware:/usr/local/zeus-pv/middleware}")
    private String middlewarePath;
    
    @PostConstruct
    public void init(){
        File file = new File(helmPath);
        for (String name : file.list()) {
            if (name.contains(".tgz")) {
                try {
                    File f = new File(helmPath + File.separator + name);
                    File targetFile = new File(middlewarePath + File.separator + name);
                    FileUtils.copyFile(f, targetFile);
                } catch (Exception e) {
                    log.error("同步中间件{} 失败", name);
                }
            }
        }
    }

}
