package com.harmonycloud.zeus.controller.middleware;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.MysqlBackupDto;
import com.harmonycloud.caas.common.model.middleware.ScheduleBackupConfig;
import com.harmonycloud.zeus.service.middleware.MysqlService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Api(tags = {"服务列表"}, value = "mysql中间件", description = "mysql中间件")
@RestController
@RequestMapping("/clusters/{clusterId}/middlewares/mysql")
public class MysqlController {

    @Autowired
    private MysqlService mysqlService;

    @ApiOperation(value = "查询数据备份列表", notes = "查询数据备份列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "mysqlName", value = "mysql名称", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/{mysqlName}/backups")
    public BaseResult<List<MysqlBackupDto>> list(@PathVariable("clusterId") String clusterId,
                                                 @RequestParam("namespace") String namespace,
                                                 @PathVariable("mysqlName") String mysqlName) {
        return BaseResult.ok(mysqlService.listBackups(clusterId, namespace, mysqlName));
    }

    @ApiOperation(value = "查询定时备份配置", notes = "查询定时备份配置")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "mysqlName", value = "mysql名称", paramType = "path", dataTypeClass = String.class),
    })
    @GetMapping("/{mysqlName}/backups/schedule")
    public BaseResult<ScheduleBackupConfig> getScheduleBackup(@PathVariable("clusterId") String clusterId,
                                           @RequestParam("namespace") String namespace,
                                           @PathVariable("mysqlName") String mysqlName) throws Exception {
        return BaseResult.ok(mysqlService.getScheduleBackups(clusterId, namespace, mysqlName));
    }

    @ApiOperation(value = "创建中间件定时备份", notes = "创建中间件定时备份")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "mysqlName", value = "mysql名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keepBackups", value = "保留备份数量", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "cron", value = "cron表达式", paramType = "query", dataTypeClass = String.class),
    })
    @PostMapping("/{mysqlName}/backups/schedule")
    public BaseResult createScheduleBackup(@PathVariable("clusterId") String clusterId,
                                           @RequestParam("namespace") String namespace,
                                           @PathVariable("mysqlName") String mysqlName,
                                           @RequestParam("keepBackups") Integer keepBackups,
                                           @RequestParam("cron") String cron ) throws Exception {
        mysqlService.createScheduleBackup(clusterId, namespace, mysqlName, keepBackups, cron);
        return BaseResult.ok();
    }

    @ApiOperation(value = "立即备份", notes = "立即备份")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "mysqlName", value = "mysql名称", paramType = "path", dataTypeClass = String.class),
    })
    @PostMapping("/{mysqlName}/backups")
    public BaseResult createBackup(@PathVariable("clusterId") String clusterId,
                                   @RequestParam("namespace") String namespace,
                                   @PathVariable("mysqlName") String mysqlName) throws Exception {
        mysqlService.createBackup(clusterId, namespace, mysqlName);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除备份", notes = "删除备份")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "mysqlName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupFileName", value = "备份文件名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "backupName", value = "备份名称", paramType = "path", dataTypeClass = String.class),
    })
    @DeleteMapping("/{mysqlName}/backups/{backupName}")
    public BaseResult deleteBackup(@PathVariable("clusterId") String clusterId,
                                   @RequestParam("namespace") String namespace,
                                   @PathVariable("mysqlName") String mysqlName,
                                   @RequestParam("backupFileName") String backupFileName,
                                   @PathVariable("backupName") String backupName) throws Exception {
        mysqlService.deleteBackup(clusterId, namespace, mysqlName, backupFileName, backupName);
        return BaseResult.ok();
    }

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
