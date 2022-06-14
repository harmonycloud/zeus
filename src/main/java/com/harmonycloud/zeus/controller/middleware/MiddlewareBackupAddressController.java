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

import java.util.List;

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
            @ApiImplicitParam(name = "clusters", value = "集群ID集合", paramType = "query", dataTypeClass = List.class),
            @ApiImplicitParam(name = "backupAddressDTO", value = "备份地址", paramType = "query", dataTypeClass = MiddlewareClusterBackupAddressDTO.class)
    })
    @PostMapping
    public BaseResult create(@RequestBody List<String> clusters,
                             @RequestBody MiddlewareClusterBackupAddressDTO backupAddressDTO) {
        middlewareBackupAddressService.createBackupAddress(clusters, backupAddressDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询备份地址", notes = "查询备份地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "keyword", value = "关键词", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult get(@RequestParam String keyword) {
        return BaseResult.ok(middlewareBackupAddressService.listBackupAddress(keyword));
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
            @ApiImplicitParam(name = "accessKeyId", value = "用户ID", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "secretAccessKey", value = "密码", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "bucketName", value = "bucket名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "endpoint", value = "地址", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "name", value = "中文名称", paramType = "query", dataTypeClass = String.class),
            @ApiImplicitParam(name = "id", value = "ID", paramType = "query", dataTypeClass = Integer.class),
    })
    @DeleteMapping
    public BaseResult delete(@RequestParam("accessKeyId") String accessKeyId,
                             @RequestParam("secretAccessKey") String secretAccessKey,
                             @RequestParam("bucketName") String bucketName,
                             @RequestParam("endpoint") String endpoint,
                             @RequestParam(value = "name", required = false) String name,
                             @RequestParam(value = "id", required = false) Integer id) {
        middlewareBackupAddressService.deleteBackupAddress(accessKeyId, secretAccessKey, bucketName, endpoint, name, id);
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
