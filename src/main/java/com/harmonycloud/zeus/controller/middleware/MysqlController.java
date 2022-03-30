package com.harmonycloud.zeus.controller.middleware;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.MysqlDbDTO;
import com.harmonycloud.caas.common.model.MysqlUserDTO;
import com.harmonycloud.caas.common.model.middleware.MysqlSlowSqlDTO;
import com.harmonycloud.caas.common.model.middleware.SlowLogQuery;
import com.harmonycloud.tool.page.PageObject;
import com.harmonycloud.zeus.service.middleware.MysqlService;
import com.harmonycloud.zeus.service.mysql.MysqlDbService;
import com.harmonycloud.zeus.service.mysql.MysqlUserService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Api(tags = {"服务列表", "服务管理"}, value = "mysql中间件", description = "mysql中间件")
@RestController
@RequestMapping("/clusters/{clusterId}/middlewares/mysql")
public class MysqlController {

    @Autowired
    private MysqlService mysqlService;
    @Autowired
    private MysqlUserService mysqlUserService;
    @Autowired
    private MysqlDbService mysqlDbService;

    @ApiOperation(value = "灾备切换", notes = "灾备切换")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class)
    })
    @PostMapping("/{middlewareName}/disasterRecovery")
    public BaseResult switchDisasterRecovery(@PathVariable("clusterId") String clusterId,
                                             @RequestParam("namespace") String namespace,
                                             @PathVariable("middlewareName") String middlewareName) {
        return mysqlService.switchDisasterRecovery(clusterId, namespace, middlewareName);
    }

    @ApiOperation(value = "查询mysql访问信息", notes = "查询mysql访问信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "mysqlName", value = "中间件名称", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/{middlewareName}/queryAccessInfo")
    public BaseResult queryAccessInfo(@PathVariable("clusterId") String clusterId,
                                      @RequestParam("namespace") String namespace,
                                      @PathVariable("middlewareName") String middlewareName) {
        return mysqlService.queryAccessInfo(clusterId, namespace, middlewareName);
    }

    @ApiOperation(value = "慢日志查询", notes = "慢日志查询")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "startTime", value = "起始时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "endTime", value = "结束时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "fromQueryTime", value = "查询时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "toQueryTime", value = "查询时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "current", value = "当前页", paramType = "query", dataTypeClass = Long.class),
            @ApiImplicitParam(name = "searchType", value = "搜索类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "searchWord", value = "关键词", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "size", value = "每页记录数", paramType = "query", dataTypeClass = Long.class),
    })
    @GetMapping("{middlewareName}/slowsql")
    public BaseResult slowsql(@PathVariable("clusterId") String clusterId,
                              @RequestParam("namespace") String namespace,
                              @PathVariable("middlewareName") String middlewareName,
                              @RequestParam("startTime") String startTime,
                              @RequestParam("endTime") String endTime,
                              @RequestParam(value = "fromQueryTime", required = false) String fromQueryTime,
                              @RequestParam(value = "toQueryTime", required = false) String toQueryTime,
                              @RequestParam(value = "current", required = false, defaultValue = "1") Integer current,
                              @RequestParam(value = "searchType", required = false) String searchType,
                              @RequestParam(value = "searchWord", required = false) String searchWord,
                              @RequestParam(value = "size", required = false, defaultValue = "10") Integer size) throws Exception {
        SlowLogQuery slowLogQuery = new SlowLogQuery();
        slowLogQuery.setStartTime(startTime);
        slowLogQuery.setEndTime(endTime);
        slowLogQuery.setCurrent(current);
        slowLogQuery.setSize(size);
        slowLogQuery.setClusterId(clusterId);
        slowLogQuery.setNamespace(namespace);
        slowLogQuery.setMiddlewareName(middlewareName);
        slowLogQuery.setFromQueryTime(fromQueryTime);
        slowLogQuery.setToQueryTime(toQueryTime);
        slowLogQuery.setSearchWord(searchWord);
        slowLogQuery.setSearchType(searchType);
        PageObject<MysqlSlowSqlDTO> slowsql = mysqlService.slowsql(slowLogQuery);
        return BaseResult.ok(slowsql.getData(), slowsql.getCount());
    }

    @ApiOperation(value = "慢日志导出", notes = "慢日志导出")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "startTime", value = "起始时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "endTime", value = "结束时间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "searchType", value = "搜索类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "searchWord", value = "关键词", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("{middlewareName}/slowsql/file")
    public void slowsqlExcel(@PathVariable("clusterId") String clusterId,
                             @RequestParam("namespace") String namespace,
                             @PathVariable("middlewareName") String middlewareName,
                             @RequestParam("startTime") String startTime,
                             @RequestParam("endTime") String endTime,
                             @RequestParam(value = "searchType", required = false) String searchType,
                             @RequestParam(value = "searchWord", required = false) String searchWord,
                             HttpServletRequest request, HttpServletResponse response) throws Exception {
        SlowLogQuery slowLogQuery = new SlowLogQuery();
        slowLogQuery.setStartTime(startTime);
        slowLogQuery.setEndTime(endTime);
        slowLogQuery.setClusterId(clusterId);
        slowLogQuery.setNamespace(namespace);
        slowLogQuery.setMiddlewareName(middlewareName);
        slowLogQuery.setSearchWord(searchWord);
        slowLogQuery.setSearchType(searchType);
        mysqlService.slowsqlExcel(slowLogQuery, response, request);
    }

    @ApiOperation(value = "创建用户", notes = "创建用户")
    @PostMapping("/createUser")
    public BaseResult createUser(@RequestBody MysqlUserDTO mysqlUserDTO) {
        return mysqlUserService.create(mysqlUserDTO);
    }

    @ApiOperation(value = "删除用户", notes = "删除用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "query", dataTypeClass = String.class)
    })
    @DeleteMapping("/deleteUser")
    public BaseResult deleteUser(@PathVariable("clusterId") String clusterId,
                                 @RequestParam("namespace") String namespace,
                                 @RequestParam("middlewareName") String middlewareName,
                                 @RequestParam("user") String user) {
        return mysqlUserService.delete(clusterId, namespace, middlewareName, user);
    }

    @ApiOperation(value = "查询用户列表", notes = "查询用户列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/listUser")
    public BaseResult listUser(@PathVariable("clusterId") String clusterId,
                               @RequestParam("namespace") String namespace,
                               @RequestParam("middlewareName") String middlewareName,
                               @RequestParam(value = "user", required = false) String user) {
        return BaseResult.ok(mysqlUserService.list(clusterId, namespace, middlewareName, user));
    }

    @ApiOperation(value = "创建数据库", notes = "创建数据库")
    @PostMapping("/createDb")
    public BaseResult createDb(@RequestBody MysqlDbDTO mysqlDbDTO) {
        mysqlDbService.create(mysqlDbDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "更新数据库备注", notes = "更新数据库备注")
    @PutMapping("/updateDb")
    public BaseResult updateDb(@RequestBody MysqlDbDTO mysqlDbDTO) {
        return mysqlDbService.update(mysqlDbDTO);
    }

    @ApiOperation(value = "查询数据库列表", notes = "查询数据库列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/listDb")
    public BaseResult listDb(@PathVariable("clusterId") String clusterId,
                             @RequestParam("namespace") String namespace,
                             @RequestParam("middlewareName") String middlewareName) {
        return BaseResult.ok(mysqlDbService.list(clusterId, namespace, middlewareName));
    }

    @ApiOperation(value = "删除数据库", notes = "删除数据库")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "query", dataTypeClass = String.class)
    })
    @DeleteMapping("/deleteDb")
    public BaseResult deleteDb(@PathVariable("clusterId") String clusterId,
                               @RequestParam("namespace") String namespace,
                               @RequestParam("middlewareName") String middlewareName,
                               @RequestParam("db") String db) {
        return mysqlDbService.delete(clusterId, namespace, middlewareName, db);
    }

    @ApiOperation(value = "授权数据库", notes = "授权数据库")
    @PostMapping("/grantUser")
    public BaseResult grantUser(@RequestBody MysqlUserDTO mysqlUserDTO) {
        return mysqlUserService.grantUser(mysqlUserDTO);
    }

    @ApiOperation(value = "修改密码", notes = "修改密码")
    @PutMapping("/updatePassword")
    public BaseResult updatePassword(@RequestBody MysqlUserDTO mysqlUserDTO) {
        return mysqlUserService.updatePassword(mysqlUserDTO);
    }

    @ApiOperation(value = "查询字符集", notes = "查询字符集")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/listCharset")
    public BaseResult listCharset(@PathVariable("clusterId") String clusterId,
                                  @RequestParam("namespace") String namespace,
                                  @RequestParam("middlewareName") String middlewareName) {
        return BaseResult.ok(mysqlDbService.listCharset(clusterId, namespace, middlewareName));
    }

}
