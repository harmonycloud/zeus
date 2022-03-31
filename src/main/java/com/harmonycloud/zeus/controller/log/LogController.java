package com.harmonycloud.zeus.controller.log;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.model.middleware.LogQuery;
import com.harmonycloud.caas.common.model.middleware.LogQueryDto;
import com.harmonycloud.zeus.annotation.Authority;
import com.harmonycloud.zeus.annotation.ExcludeAuditMethod;
import com.harmonycloud.zeus.service.log.LogService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;

/**
 * @author liyinlong
 * @description 应用日志相关控制器
 * @date 2021/6/17 5:48 下午
 */
@Controller
@Api(tags = {"监控告警", "日志详情"}, value = "应用日志")
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/middlewares/{middlewareName}/applogs")
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
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    @Authority(power = 3)
    public BaseResult queryLog(@PathVariable("clusterId") String clusterId,
                               @PathVariable("namespace") String namespace,
                               @PathVariable("middlewareName") String middlewareName,
                               @ModelAttribute LogQueryDto logQueryDto) {
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
    @RequestMapping(value = "/export", method = RequestMethod.GET)
    @Authority(power = 3)
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
    @ResponseBody
    @RequestMapping(value = "/filenames", method = RequestMethod.GET)
    @Authority(power = 3)
    public BaseResult listLogFilenames(@PathVariable("clusterId") String clusterId,
                                       @PathVariable("namespace") String namespace,
                                       @PathVariable("middlewareName") String middlewareName,
                                       @ModelAttribute LogQueryDto logQueryDto) {
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


}
