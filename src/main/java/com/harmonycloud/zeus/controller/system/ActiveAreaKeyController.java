package com.harmonycloud.zeus.controller.system;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.AffinityDTO;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author xutianhong
 * @Date 2022/11/10 8:23 下午
 */
@Api(tags = {"集群管理","可用区"}, value = "集群可用区", description = "集群可用区")
@RestController
@RequestMapping("/area")
public class ActiveAreaKeyController {

    @Value("${active-active.label.key:topology.kubernetes.io/zone}")
    private String zoneKey;
    @Value("${active-active.label.value:zoneC}")
    private String zoneValue;
    @Value("${active-active.tolerations:harm.cn/type=active-active:NoSchedule}")
    private String tolerations;

    @ApiOperation(value = "获取可用区key", notes = "获取可用区key")
    @ApiImplicitParams({
    })
    @GetMapping("/keys")
    public BaseResult<AffinityDTO> key() {
        String zone = zoneKey + "=" + zoneValue;
        AffinityDTO affinityDTO = new AffinityDTO();
        affinityDTO.setLabel(zone);
        affinityDTO.setRequired(true);
        affinityDTO.setAnti(true);
        return BaseResult.ok(affinityDTO);
    }

    @ApiOperation(value = "获取可用区tolerations", notes = "获取可用区tolerations")
    @ApiImplicitParams({
    })
    @GetMapping("/tolerations")
    public BaseResult<String> tolerations() {
        return BaseResult.ok(tolerations);
    }

}
