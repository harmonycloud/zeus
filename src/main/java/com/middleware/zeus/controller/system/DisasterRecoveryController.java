package com.middleware.zeus.controller.system;

import com.middleware.caas.common.model.DisasterRecoveryInfo;
import com.middleware.zeus.service.system.PlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.middleware.caas.common.base.BaseResult;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @author xutianhong
 * @Date 2022/11/22 1:54 下午
 */
@Api(tags = {"平台管理", "灾备中心"}, value = "平台管理")
@RestController
@RequestMapping("/platform/disasterRecovery/")
public class DisasterRecoveryController {

    @Autowired
    private PlatformService platformService;

    @ApiOperation(value = "查询平台访问信息", notes = "查询平台访问信息")
    @GetMapping("queryAccessInfo")
    public BaseResult queryAccessInfo() {
        return BaseResult.ok(platformService.queryAccessInfo());
    }

    @ApiOperation(value = "存储主备平台访问信息", notes = "存储主备平台访问信息")
    @PostMapping("/{name}")
    public BaseResult saveSpareAddr(@RequestBody DisasterRecoveryInfo disasterRecoveryInfo, @PathVariable String name) {
        platformService.saveAddr(disasterRecoveryInfo, name);
        return BaseResult.ok();
    }

    @ApiOperation(value = "平台主备切换", notes = "平台主备切换")
    @PostMapping("switch")
    public BaseResult switchPlatform(HttpServletRequest request) throws IOException {
        platformService.switchPlatform(request);
        return BaseResult.ok();
    }

}
