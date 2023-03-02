package com.middleware.zeus.controller.k8s;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.zeus.service.k8s.SecretService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author xutianhong
 * @since 2021/6/23 10:53 上午
 */
@Api(tags = "secret", value = "secret资源", description = "secret资源")
@RestController
@RequestMapping("/clusters/{clusterId}//namespaces/{namespace}/secret")
public class SecretController {

    @Autowired
    private SecretService secretService;

    @ApiOperation(value = "查询secret列表", notes = "查询secret列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult list(@PathVariable("clusterId") String clusterId,
                           @PathVariable("namespace") String namespace) {
        return BaseResult.ok(secretService.list(clusterId, namespace));
    }

}
