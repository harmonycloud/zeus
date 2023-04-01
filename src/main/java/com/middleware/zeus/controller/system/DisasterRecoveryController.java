package com.middleware.zeus.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.DisasterRecoveryInfo;
import com.middleware.zeus.service.system.PlatformService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

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
    @ApiImplicitParams({
            @ApiImplicitParam(name = "disasterRecoveryInfo", value = "平台灾备业务对象", paramType = "query", dataTypeClass = DisasterRecoveryInfo.class),
    })
    @PostMapping
    public BaseResult saveSpareAddr(@RequestBody DisasterRecoveryInfo disasterRecoveryInfo) {
        platformService.saveAddr(disasterRecoveryInfo);
        return BaseResult.ok();
    }

    @ApiOperation(value = "平台主备切换", notes = "平台主备切换")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "isMaster", value = "是否主平台", paramType = "query", dataTypeClass = Boolean.class),
    })
    @PostMapping("switch")
    public BaseResult switchPlatform(@RequestParam("isMaster") Boolean isMaster) {
        platformService.switchPlatform(isMaster);
        return BaseResult.ok();
    }

    @ApiOperation(value = "同步器运行状态", notes = "同步器运行状态")
    @GetMapping("replicate")
    public BaseResult getMysqlReplicateStatus(){
        return BaseResult.ok(platformService.getMysqlReplicateStatus());
    }

    @ApiOperation(value = "同步器运行状态", notes = "同步器运行状态")
    @GetMapping("mysql")
    public BaseResult getRelationMysqlPhase(){
        return BaseResult.ok(platformService.getRelationMysqlPhase());
    }

}
