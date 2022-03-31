package com.harmonycloud.zeus.controller.middleware;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.zeus.annotation.Authority;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/24
 */
@Api(tags = "clusterMiddleware", value = "集群下中间件", description = "集群下中间件")
@RestController
@RequestMapping("/clusters/{clusterId}/middlewares")
public class ClusterMiddlewareController {

    @Autowired
    private MiddlewareService middlewareService;

    @ApiOperation(value = "查询中间件列表", notes = "查询中间件列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyword", value = "模糊搜索", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping
    @Authority
    public BaseResult<List<Middleware>> list(@PathVariable("clusterId") String clusterId,
                                             @RequestParam(value = "type", required = false) String type,
                                             @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResult.ok(middlewareService.simpleList(clusterId, null, type, keyword));
    }


}
