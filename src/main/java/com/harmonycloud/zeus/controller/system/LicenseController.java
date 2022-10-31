package com.harmonycloud.zeus.controller.system;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.LicenseInfoDto;
import com.harmonycloud.tool.file.FileUtil;
import com.harmonycloud.zeus.service.system.LicenseService;
import io.fabric8.kubernetes.client.ConfigBuilder;
import io.fabric8.kubernetes.client.DefaultKubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
    @GetMapping("/check/middleware")
    public BaseResult middleware() throws Exception {
        licenseService.middlewareResource();
        return BaseResult.ok();
    }

    @ApiOperation(value = "测试", notes = "测试")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "license", value = "license", paramType = "query", dataTypeClass = String.class),
    })
    @GetMapping("/check/namespace")
    public BaseResult namespace() throws Exception {
        String token = FileUtil.readFile("/var/run/secrets/kubernetes.io/serviceaccount/token");
        log.info("token: {}", token);
        KubernetesClient client = new DefaultKubernetesClient(new ConfigBuilder().withMasterUrl("https://10.96.0.1:443")
                .withTrustCerts(true).withOauthToken(token).build());
        io.fabric8.kubernetes.api.model.Namespace namespace = client.namespaces().withName("kube-system").get();
        log.info(namespace.getMetadata().getUid());
        return BaseResult.ok();
    }

}
