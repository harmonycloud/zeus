package com.middleware.zeus.controller.k8s;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.EventDetail;
import com.middleware.zeus.service.k8s.EventService;
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
 * @date 2021/04/25
 */
@Api(tags = "event", value = "事件", description = "事件")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces/{namespace}/middlewares")
public class EventController {

    @Autowired
    private EventService eventService;


    @ApiOperation(value = "查询最近事件", notes = "查询最近事件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/events")
    public BaseResult<List<EventDetail>> listEvents(@PathVariable("clusterId") String clusterId,
                                                    @PathVariable("namespace") String namespace) {
        return BaseResult.ok(eventService.getEvents(clusterId, namespace));
    }

    @ApiOperation(value = "查询最近事件", notes = "查询最近事件")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "middlewareName", value = "中间件名称", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "type", value = "中间件类型", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "eventType", value = "事件状态", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "kind", value = "资源类型", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping("/{middlewareName}/events")
    public BaseResult<List<EventDetail>> listEvents(@PathVariable("clusterId") String clusterId,
                                                    @PathVariable("namespace") String namespace,
                                                    @PathVariable("middlewareName") String middlewareName,
                                                    @RequestParam("type") String middlewareType,
                                                    @RequestParam(value = "eventType", required = false) String eventType,
                                                    @RequestParam(value = "kind", required = false) String kind) {
        return BaseResult.ok(eventService.getEvents(clusterId, namespace, middlewareName, middlewareType, eventType, kind));
    }


}
