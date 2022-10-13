package com.harmonycloud.zeus.controller.dashboard;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.dashboard.DatabaseDto;
import com.harmonycloud.caas.common.model.dashboard.MiddlewareUserAuthority;
import com.harmonycloud.caas.common.model.dashboard.MiddlewareUserDto;
import com.harmonycloud.zeus.service.dashboard.PostgresqlDashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/10/10 3:29 下午
 */
@Api(tags = {"服务列表", "服务管理"}, value = "mysql中间件", description = "mysql中间件")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/postgresql/{name}")
public class PostgresqlDashboardController {

    @Autowired
    private PostgresqlDashboardService postgresqlDashboardService;

    @ApiOperation(value = "获取database列表", notes = "获取database列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/databases")
    public BaseResult<List<DatabaseDto>> listDatabase(@PathVariable("clusterId") String clusterId,
                                                      @PathVariable("namespace") String namespace,
                                                      @PathVariable("name") String name) {
        return BaseResult.ok(postgresqlDashboardService.listDatabases(clusterId, namespace, name));
    }

    @ApiOperation(value = "获取schema列表", notes = "获取schema列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/database/{databaseName}/schemas")
    public BaseResult listSchema(@PathVariable("clusterId") String clusterId,
                                 @PathVariable("namespace") String namespace,
                                 @PathVariable("name") String name,
                                 @PathVariable("databaseName") String databaseName) {
        return BaseResult.ok(postgresqlDashboardService.listDatabases(clusterId, namespace, name));
    }

    @ApiOperation(value = "获取table列表", notes = "获取table列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/database/{database}/schemas/{schema}/tables")
    public BaseResult listTables(@PathVariable("clusterId") String clusterId,
                                 @PathVariable("namespace") String namespace,
                                 @PathVariable("name") String name,
                                 @PathVariable("database") String database,
                                 @PathVariable("schema") String schema) {
        return BaseResult.ok(postgresqlDashboardService.listDatabases(clusterId, namespace, name));
    }

    @ApiOperation(value = "获取用户列表", notes = "获取用户列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyword", value = "关键词", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/user")
    public BaseResult<List<MiddlewareUserDto>> listUser(@PathVariable("clusterId") String clusterId,
                                                        @PathVariable("namespace") String namespace,
                                                        @PathVariable("name") String name,
                                                        @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResult.ok(postgresqlDashboardService.listUser(clusterId, namespace, name, keyword));
    }

    @ApiOperation(value = "创建用户", notes = "创建用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareUserDto", value = "用户对象", paramType = "query", dataTypeClass = MiddlewareUserDto.class),
    })
    @PostMapping("/user")
    public BaseResult addUser(@PathVariable("clusterId") String clusterId,
                              @PathVariable("namespace") String namespace,
                              @PathVariable("name") String name,
                              @RequestBody MiddlewareUserDto middlewareUserDto) {
        postgresqlDashboardService.addUser(clusterId, namespace, name, middlewareUserDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除用户", notes = "删除用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名称", paramType = "path", dataTypeClass = String.class),
    })
    @DeleteMapping("/user/{username}")
    public BaseResult dropUser(@PathVariable("clusterId") String clusterId,
                               @PathVariable("namespace") String namespace,
                               @PathVariable("name") String name,
                               @PathVariable("username") String username) {
        postgresqlDashboardService.dropUser(clusterId, namespace, name, username);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询用户权限", notes = "查询用户权限")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "username", value = "用户名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/user/{username}/authority")
    public BaseResult<List<MiddlewareUserAuthority>> userAuthority(@PathVariable("clusterId") String clusterId,
                                                                   @PathVariable("namespace") String namespace,
                                                                   @PathVariable("name") String name,
                                                                   @PathVariable("username") String username) {
        return BaseResult.ok(postgresqlDashboardService.userAuthority(clusterId, namespace, name, username));
    }



}
