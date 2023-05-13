package com.middleware.zeus.controller.k8s;

import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.model.ClusterQuotaQuery;
import com.middleware.zeus.service.k8s.ClusterQuotaService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author xutianhong
 * @Date 2023/3/28 4:13 下午
 */
@Slf4j
@Api(tags = {"系统管理","基础资源"}, value = "平台", description = "平台")
@RestController
@RequestMapping("/clusterQuota")
public class ClusterQuotaController {

    @Autowired
    private ClusterQuotaService clusterQuotaService;

    @ApiOperation(value = "查询平台集群资源配额情况", notes = "查询平台集群资源配额情况")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterQuotaQuery", value = "资源查询对象", paramType = "query", dataTypeClass = ClusterQuotaQuery.class),
    })
    @PostMapping
    public BaseResult getResourceQuotaInfo(@RequestBody ClusterQuotaQuery clusterQuotaQuery){
        return BaseResult.ok(clusterQuotaService.list(clusterQuotaQuery));
    }

}
