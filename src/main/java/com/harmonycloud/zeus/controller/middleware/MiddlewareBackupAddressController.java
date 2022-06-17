package com.harmonycloud.zeus.controller.middleware;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterBackupAddressDTO;
import com.harmonycloud.zeus.service.middleware.MiddlewareBackupAddressService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author yushuaikang
 * @date 2022/6/9 17:34 下午
 */
@Api(tags = {"备份服务", "备份位置"}, value = "备份位置")
@RestController
@RequestMapping("/backup/address")
public class MiddlewareBackupAddressController {

    @Autowired
    private MiddlewareBackupAddressService middlewareBackupAddressService;

    @ApiOperation(value = "创建备份地址", notes = "创建备份地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "backupDTO", value = "备份地址", paramType = "query", dataTypeClass = MiddlewareClusterBackupAddressDTO.class)
    })
    @PostMapping
    public BaseResult create(@RequestBody MiddlewareClusterBackupAddressDTO middlewareBackupDTO) {
        middlewareBackupAddressService.createBackupAddress(middlewareBackupDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询备份地址", notes = "查询备份地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "keyword", value = "关键词", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中文名", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult get(@RequestParam(value = "keyword", required = false) String keyword,
                          @RequestParam(value = "name", required = false) String name) {
        return BaseResult.ok(middlewareBackupAddressService.listBackupAddress(name, keyword));
    }

    @ApiOperation(value = "查询备份地址详情", notes = "查询备份地址详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "主键", paramType = "query", dataTypeClass = Integer.class),
    })
    @GetMapping("/detail")
    public BaseResult detail(@RequestParam Integer id) {
        return BaseResult.ok(middlewareBackupAddressService.detail(id));
    }

    @ApiOperation(value = "修改备份地址", notes = "修改备份地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "backupAddressDTO", value = "备份地址", paramType = "query", dataTypeClass = MiddlewareClusterBackupAddressDTO.class)
    })
    @PutMapping
    public BaseResult update(@RequestBody MiddlewareClusterBackupAddressDTO backupAddressDTO) {
        middlewareBackupAddressService.updateBackupAddress(backupAddressDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除备份地址", notes = "删除备份地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "ID", paramType = "query", dataTypeClass = Integer.class),
            @ApiImplicitParam(name = "clusterId", value = "集群ID", paramType = "query", dataTypeClass = String.class),
    })
    @DeleteMapping
    public BaseResult delete(@RequestParam(value = "id", required = false) Integer id,
                             @RequestParam(value = "clusterId", required = false) String clusterId) {
        middlewareBackupAddressService.deleteBackupAddress(id, clusterId);
        return BaseResult.ok();
    }

    @ApiOperation(value = "minio连接校验", notes = "minio连接校验")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "backupAddressDTO", value = "备份地址", paramType = "query", dataTypeClass = MiddlewareClusterBackupAddressDTO.class)
    })
    @PostMapping("/check")
    public BaseResult check(@RequestBody MiddlewareClusterBackupAddressDTO backupAddressDTO) {
        middlewareBackupAddressService.checkMinio(backupAddressDTO);
        return BaseResult.ok();
    }

}
