package com.harmonycloud.zeus.controller.middleware;

import com.harmonycloud.zeus.service.middleware.MqService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Api(tags = "mq", value = "mq中间件", description = "mq中间件")
@RestController
@RequestMapping("/clusters/{clusterId}/middlewares/mq")
public class MqController {

    @Autowired
    private MqService mqService;


}
