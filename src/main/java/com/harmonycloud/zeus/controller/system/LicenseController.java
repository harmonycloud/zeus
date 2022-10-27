package com.harmonycloud.zeus.controller.system;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.zeus.service.system.LicenseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author xutianhong
 * @Date 2022/10/27 11:41 上午
 */
@Api(tags = {"系统管理", "平台认证"}, value = "平台认证")
@RestController
@RequestMapping("/license")
public class LicenseController {

    @Autowired
    private LicenseService licenseService;

    @ApiOperation(value = "license认证", notes = "license认证")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "license", value = "license", paramType = "query", dataTypeClass = String.class),
    })
    @PostMapping
    public BaseResult authentication(@RequestParam("license") String license) {
        licenseService.license(license);
        return BaseResult.ok();
    }

    @ApiOperation(value = "license认证", notes = "license认证")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "license", value = "license", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/check")
    public BaseResult<Boolean> check() {
        return BaseResult.ok(licenseService.check());
    }

}
