package com.harmonycloud.zeus.controller.middleware;

import com.harmonycloud.zeus.service.middleware.EsService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Api(tags = "es", value = "es中间件", description = "es中间件")
@RestController
@RequestMapping("/clusters/{clusterId}/middlewares/es")
public class EsController {

    @Autowired
    private EsService esService;


}
