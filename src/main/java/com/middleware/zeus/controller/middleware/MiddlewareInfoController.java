package com.middleware.zeus.controller.middleware;

import com.dtflys.forest.annotation.Post;
import com.middleware.zeus.common.model.middleware.MiddlewareDisableVersionDto;
import com.middleware.zeus.common.model.middleware.MiddlewareDisableVersionDtoList;
import com.middleware.zeus.service.middleware.MiddlewareInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.model.MiddlewareVersionDto;
import com.middleware.zeus.common.model.middleware.MiddlewareInfoDTO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Api(tags = {"中间件市场"}, value = "中间件信息", description = "中间件信息")
@RestController
@RequestMapping("/middlewares/info")
public class MiddlewareInfoController {

    @Autowired
    private MiddlewareInfoService middlewareInfoService;

    @ApiOperation(value = "查询可用的中间件列表", notes = "查询可用的中间件列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult list(@RequestParam(value = "clusterId", required = false) String clusterId) {
        return BaseResult.ok(middlewareInfoService.list(clusterId));
    }

    @ApiOperation(value = "查询可发布的中间件信息", notes = "查询可发布的中间件信息")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "类型", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/{type}")
    public BaseResult<MiddlewareInfoDTO> get(@RequestParam(value = "clusterId") String clusterId,
                                             @PathVariable("type") String type) {
        return BaseResult.ok(middlewareInfoService.getByType(clusterId, type));
    }

    @ApiOperation(value = "查询指定中间件版本", notes = "查询指定中间件版本")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "类型", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/version")
    public BaseResult chartVersion(@RequestParam(value = "clusterId") String clusterId,
                                   @RequestParam(value = "type") String type) {
        return BaseResult.ok(middlewareInfoService.chartVersion(clusterId, type));
    }

    @ApiOperation(value = "中间件下架", notes = "中间件下架")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "chartName", value = "名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "chartVersion", value = "版本", paramType = "query", dataTypeClass = String.class)
    })
    @DeleteMapping("/delete")
    public BaseResult delete(@RequestParam(value = "chartName") String chartName,
                              @RequestParam(value = "chartVersion") String chartVersion) {
        middlewareInfoService.delete(chartName, chartVersion);
        return BaseResult.ok();
    }

    @ApiOperation(value = "已纳管集群服务", notes = "已纳管集群服务")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyword", value = "关键词", paramType = "query", dataTypeClass = String.class)})
    @GetMapping("/list")
    public BaseResult middlewareList(@RequestParam(value = "type", required = false) String type,
                                     @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResult.ok(middlewareInfoService.middlewareList(type, keyword));
    }

    @ApiOperation(value = "已纳管集群下中间件", notes = "已纳管集群下中间件")
    @GetMapping("/middleware")
    public BaseResult clusterList() {
        return BaseResult.ok(middlewareInfoService.clusterList());
    }

    @ApiOperation(value = "查询指定中间件发布时可指定版本", notes = "查询指定中间件发布时可指定版本")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "类型", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "chartVersion", value = "chart版本", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "filter", value = "是否过滤隐藏", required = false, paramType = "query", dataTypeClass = Boolean.class),
    })
    @GetMapping("/{type}/version")
    public BaseResult<List<MiddlewareVersionDto>> version(@PathVariable("type") String type,
                                                          @RequestParam("chartVersion") String chartVersion,
                                                          @RequestParam("clusterId") String clusterId,
                                                          @RequestParam(value = "filter", required = false, defaultValue = "true") Boolean filter) {
        return BaseResult.ok(middlewareInfoService.version(type, chartVersion, clusterId, filter));
    }

    @ApiOperation(value = "查询指定中间件的禁用版本列表", notes = "查询指定中间件发布时的禁用版本列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "类型", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "clusterId", value = "chart版本", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "chartVersion", value = "chart版本", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/{type}/disableVersion")
    public BaseResult<List<MiddlewareDisableVersionDto>> disableVersion(@PathVariable("type") String type,
                                                                        @RequestParam("clusterId") String clusterId,
                                                                        @RequestParam("chartVersion") String chartVersion) {
        return BaseResult.ok(middlewareInfoService.disableVersion(clusterId, type, chartVersion));
    }

    @ApiOperation(value = "设置中间件禁用版本", notes = "设置中间件禁用版本")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "type", value = "类型", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareDisableVersionDtoList", value = "禁用版本列表", paramType = "query", dataTypeClass = MiddlewareDisableVersionDtoList.class)
    })
    @PostMapping("/{type}/disableVersion")
    public BaseResult addDisableVersion(@PathVariable("type") String type,
                                        @RequestBody MiddlewareDisableVersionDtoList middlewareDisableVersionDtoList) {
        middlewareInfoService.setDisableVersion(middlewareDisableVersionDtoList.getMiddlewareDisableVersionDtoList());
        return BaseResult.ok();
    }
}
