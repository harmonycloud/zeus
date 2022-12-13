package com.middleware.zeus.controller.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.middleware.caas.common.base.BaseResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * @author xutianhong
 * @Date 2022/11/22 1:54 下午
 */
@Api(tags = {"平台管理", "灾备中心"}, value = "平台管理")
@RestController
@RequestMapping("/system/disasterRecovery")
public class DisasterRecoveryController {

    @Value("${system.disasterRecovery:true}")
    private String enable;

    @ApiOperation(value = "灾备是否启用", notes = "灾备是否启用")
    @GetMapping("/enable")
    public BaseResult enable() {
        return BaseResult.ok(enable);
    }

}
