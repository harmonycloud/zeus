package com.harmonycloud.zeus.controller.middleware;

import com.harmonycloud.zeus.service.middleware.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.annotations.Api;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Api(tags = "redis", value = "redis中间件", description = "redis中间件")
@RestController
@RequestMapping("/clusters/{clusterId}/middlewares/redis")
public class RedisController {

    @Autowired
    private RedisService redisService;


}
