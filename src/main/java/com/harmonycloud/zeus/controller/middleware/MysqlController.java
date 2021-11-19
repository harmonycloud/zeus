package com.harmonycloud.zeus.controller.middleware;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.zeus.service.middleware.MysqlService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Api(tags = {"服务列表","服务管理"}, value = "mysql中间件", description = "mysql中间件")
@RestController
@RequestMapping("/clusters/{clusterId}/middlewares/mysql")
public class MysqlController {

    @Autowired
    private MysqlService mysqlService;

    @ApiOperation(value = "灾备切换", notes = "灾备切换")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "mysqlName", value = "中间件名称", paramType = "path", dataTypeClass = String.class)
    })
    @PostMapping("/{mysqlName}/disasterRecovery")
    public BaseResult switchDisasterRecovery(@PathVariable("clusterId") String clusterId,
                                             @RequestParam("namespace") String namespace,
                                             @PathVariable("mysqlName") String mysqlName){
        return mysqlService.switchDisasterRecovery(clusterId, namespace, mysqlName);
    }

    @ApiOperation(value = "查询mysql访问信息", notes = "查询mysql访问信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "mysqlName", value = "中间件名称", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/{mysqlName}/queryAccessInfo")
    public BaseResult queryAccessInfo(@PathVariable("clusterId") String clusterId,
                                      @RequestParam("namespace") String namespace,
                                      @PathVariable("mysqlName") String mysqlName) {
        return mysqlService.queryAccessInfo(clusterId, namespace, mysqlName);
    }
}
