package com.middleware.zeus.controller;

import com.middleware.zeus.bean.PlatformOverviewDTO;
import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.service.middleware.MiddlewareAlertsService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author xutianhong
 * @Date 2025/7/4 16:32
 */
@Api(tags = "HKUpgradeController", value = "hk升级所需接口", description = "hk升级所需接口")
@RestController
@RequestMapping("/v138/upgrade")
public class HKUpgradeController {

    @Autowired
    private MiddlewareAlertsService middlewareAlertsService;

    @ApiOperation(value = "告警规则刷新", notes = "告警规则刷新")
    @PostMapping
    public BaseResult<PlatformOverviewDTO> refreshAlert() {
        middlewareAlertsService.refresh();
        return BaseResult.ok();
    }

}
