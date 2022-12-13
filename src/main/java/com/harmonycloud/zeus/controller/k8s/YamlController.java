package com.harmonycloud.zeus.controller.k8s;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.YamlCheck;
import com.harmonycloud.zeus.service.k8s.YamlService;
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

}
