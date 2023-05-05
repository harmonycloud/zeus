package com.middleware.zeus.controller.system;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.FeatureDto;
import com.middleware.caas.common.model.LdapConfigDto;
import com.middleware.zeus.service.system.FeatureService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2023/5/4 7:19 下午
 */
@Api(tags = {"平台管理", "功能列表"}, value = "平台管理")
@RestController
@RequestMapping("/feature")
public class FeatureController {

    @Autowired
    private FeatureService featureService;

    @ApiOperation(value = "查询feature功能列表", notes = "查询feature功能列表")
    @GetMapping
    public BaseResult<List<FeatureDto>> enable() {
        return BaseResult.ok(featureService.list());
    }

}
