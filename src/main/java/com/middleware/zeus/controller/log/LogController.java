package com.middleware.zeus.controller.log;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.util.page.PageObject;
import com.middleware.zeus.common.model.middleware.LogQuery;
import com.middleware.zeus.common.model.middleware.LogQueryDto;
import com.middleware.zeus.common.model.middleware.MiddlewareLogQuery;
import com.middleware.zeus.common.model.middleware.MysqlLogDTO;
import com.middleware.zeus.annotation.Authority;
import com.middleware.zeus.annotation.ExcludeAuditMethod;
import com.middleware.zeus.service.log.LogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.util.List;

/**
 * @author liyinlong
 * @description 应用日志相关控制器
 * @date 2021/6/17 5:48 下午
 */
@RestController
@Api(tags = {"服务列表", "日志详情"}, value = "应用日志")
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/middlewares/{middlewareName}")
public class LogController {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private LogService logService;

    /**
     * @description 查询pod日志
     * @author liyinlong
     * @date 2021/6/17 5:48 下午
     */
    @ExcludeAuditMethod
    @ApiOperation(value = "查询日志", notes = "查询日志")
    @PostMapping("/applogs")
    @Authority(power = 1)
    public BaseResult queryLog(@PathVariable("clusterId") String clusterId,
                               @PathVariable("namespace") String namespace,
                               @PathVariable("middlewareName") String middlewareName,
                               @RequestBody LogQueryDto logQueryDto) {
        try {
            logQueryDto.setClusterId(clusterId);
            logQueryDto.setNamespace(namespace);
            logQueryDto.setMiddlewareName(middlewareName);
            logger.info(logQueryDto.isPodLog() ? "查询pod标准输出,logQuery:{}" : "查询文件日志内容,logQuery:{}",
                    JSONObject.toJSONString(logQueryDto));
            LogQuery logQuery = logService.transLogQuery(logQueryDto);
            return logService.getLogContents(logQuery);
        } catch (Exception e) {
            logger.error("根据日志路径获取container日志失败：logQueryDto:{}",
                    logQueryDto.toString(), e);
            return BaseResult.error(ErrorMessage.UNKNOWN);
        }

    }

    /**
     * @description 导出查询日志
     * @author liyinlong
     * @date 2021/6/21 5:05 下午
     */
    @ApiOperation(value = "导出日志", notes = "导出查询日志")
    @GetMapping("/applogs/export")
    @Authority(power = 1)
    public void exportLog(@PathVariable("clusterId") String clusterId,
                          @PathVariable("namespace") String namespace,
                          @PathVariable("middlewareName") String middlewareName,
                          @ModelAttribute LogQueryDto logQueryDto,
                          HttpServletResponse response) throws Exception {
        logQueryDto.setClusterId(clusterId);
        logQueryDto.setNamespace(namespace);
        logQueryDto.setMiddlewareName(middlewareName);
        logger.info("导出日志,params:{} ", logQueryDto.toString());
        LogQuery logQuery = logService.transLogQuery(logQueryDto);
        logService.exportLog(logQuery, response);
    }

    /**
     * @description 查询pod日志文件列表
     * @author liyinlong
     * @date 2021/6/21 5:05 下午
     */
    @ApiOperation(value = "查询pod日志文件列表", notes = "从es获取pod的日志文件列表")
    @PostMapping("/applogs/filenames")
    @Authority(power = 1)
    public BaseResult listLogFilenames(@PathVariable("clusterId") String clusterId,
                                       @PathVariable("namespace") String namespace,
                                       @PathVariable("middlewareName") String middlewareName,
                                       @RequestBody LogQueryDto logQueryDto) {
        try {
            logQueryDto.setClusterId(clusterId);
            logQueryDto.setNamespace(namespace);
            logQueryDto.setMiddlewareName(middlewareName);
            logger.info("获取pod的日志文件列表,logQuery:{}", JSONObject.toJSONString(logQueryDto));
            LogQuery logQuery = logService.transLogQuery(logQueryDto);
            return logService.listfileName(logQuery);
        } catch (Exception e) {
            logger.error("获取pod的日志文件列表失败：middlewareName:{}", middlewareName, e);
            return BaseResult.error(ErrorMessage.ELASTICSEARCH_CONNECT_FAILED);
        }
    }

    @ApiOperation(value = "查询审计日志", notes = "查询审计日志")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "auditLogQuery", value = "中间件日志查询", paramType = "query", dataTypeClass = MiddlewareLogQuery.class),
    })
    @Authority(power = 1)
    @PostMapping("/applogs/audit")
    public BaseResult<PageObject<MysqlLogDTO>> queryAuditSql(@PathVariable("clusterId") String clusterId,
                                                             @PathVariable("namespace") String namespace,
                                                             @PathVariable("middlewareName") String middlewareName,
                                                             @RequestBody MiddlewareLogQuery auditLogQuery) throws Exception {
        auditLogQuery.setClusterId(clusterId).setNamespace(namespace).setMiddlewareName(middlewareName);
        return BaseResult.ok(logService.andit(auditLogQuery));
    }

    @ApiOperation(value = "查询索引列信息", notes = "查询索引列信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "index", value = "索引", paramType = "path", dataTypeClass = String.class),
    })
    @Authority(power = 1)
    @GetMapping("/index/{index}/column")
    public BaseResult<List<String>> getIndexColumnInfo(@PathVariable("clusterId") String clusterId,
                                                       @PathVariable("namespace") String namespace,
                                                       @PathVariable("middlewareName") String middlewareName,
                                                       @PathVariable("index") String index) {
        return BaseResult.ok(logService.getIndexColumnInfo(clusterId, namespace, index));
    }




}
