package com.middleware.zeus.config;

import com.middleware.zeus.service.middleware.MiddlewareInfoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

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
