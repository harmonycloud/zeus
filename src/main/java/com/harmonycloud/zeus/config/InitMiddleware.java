package com.harmonycloud.zeus.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import javax.annotation.PostConstruct;

import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.common.util.ByteUtils;
import org.springframework.beans.factory.annotation.Autowired;
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


    @Value("${system.images.path:/usr/local/zeus-pv/images/middleware}")
    private String imagePath;

    @Autowired
    private MiddlewareInfoService middlewareInfoService;
    
    @PostConstruct
    public void init() throws Exception{
    }

}
