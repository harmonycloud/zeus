package com.middleware.zeus.controller.k8s;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.YamlCheck;
import com.middleware.zeus.service.k8s.YamlService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author xutianhong
 * @Date 2021/12/23 3:22 下午
 */
@Api(tags = {"平台工具箱", "格式校验"}, value = "yaml文件", description = "yaml文件")
@RestController
@RequestMapping("/yaml")
public class YamlController {

    @Autowired
    private YamlService yamlService;

    @ApiOperation(value = "yaml格式校验", notes = "yaml格式校验")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "yaml", value = "yaml内容", paramType = "query", dataTypeClass = String.class)
    })
    @PostMapping("/verification")
    public BaseResult<YamlCheck> check(@RequestParam("yaml") String yaml) {
        return BaseResult.ok(yamlService.check(yaml));
    }

    @ApiOperation(value = "查看资源yaml", notes = "查看资源yaml")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "分区id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "plural", value = "资源类型", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "资源名称", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/view")
    public BaseResult<String> yaml(@RequestParam("clusterId") String clusterId,
                                   @RequestParam("namespace") String namespace,
                                   @RequestParam("plural") String plural,
                                   @RequestParam("name") String name) {
        return BaseResult.ok(yamlService.view(clusterId, namespace, plural, name));
    }

}
