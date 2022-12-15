package com.middleware.zeus.controller.system;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.LicenseInfoDto;
import com.middleware.zeus.service.system.LicenseService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

/**
 * @author xutianhong
 * @Date 2022/10/27 11:41 上午
 */
@Api(tags = {"系统管理", "平台认证"}, value = "平台认证")
@RestController
@RequestMapping("/license")
@Slf4j
public class LicenseController {

    @Value("${system.license.enable: true}")
    private String enable;

    @Autowired
    private LicenseService licenseService;

    @ApiOperation(value = "license认证", notes = "license认证")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "license", value = "license", paramType = "query", dataTypeClass = String.class),
    })
    @PostMapping
    public BaseResult authentication(@RequestParam("license") String license) throws Exception {
        licenseService.license(license);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询license使用信息", notes = "查询license使用信息")
    @ApiImplicitParams({
    })
    @GetMapping
    public BaseResult<LicenseInfoDto> info() throws Exception {
        return BaseResult.ok(licenseService.info());
    }

    @ApiOperation(value = "查询license使用信息", notes = "查询license使用信息")
    @ApiImplicitParams({
    })
    @GetMapping("/enable")
    public BaseResult<String> enable() throws Exception {
        return BaseResult.ok(enable);
    }

    @ApiOperation(value = "发布中间件能力校验", notes = "发布中间件能力校验")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "license", value = "license", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/check")
    public BaseResult<Boolean> check(@RequestParam("clusterId") String clusterId) throws Exception {
        return BaseResult.ok(licenseService.check(clusterId));
    }

    @ApiOperation(value = "发布中间件能力校验", notes = "发布中间件能力校验")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "license", value = "license", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/check/test")
    public BaseResult<Boolean> test() throws Exception {
        for (int i = 0; i < 3; ++i){
            licenseService.refreshMiddlewareResource();
        }
        return BaseResult.ok();
    }

}
