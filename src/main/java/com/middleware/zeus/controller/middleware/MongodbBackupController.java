package com.middleware.zeus.controller.middleware;

import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbScheduleBackupDto;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbBackupServerDto;
import com.middleware.zeus.service.middleware.MongodbBackupService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author xutianhong
 * @Date 2025/7/31 10:27
 */
@Api(tags = "mongodbBackup", value = "mongodb备份", description = "mongodb备份")
@RestController
@RequestMapping("")
public class MongodbBackupController {

    @Autowired
    private MongodbBackupService mongodbBackupService;

    @ApiOperation(value = "控制面配置mongodb备份服务器地址", notes = "控制面配置mongodb备份服务器地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "MongodbBackupServerDto", value = "mongodb备份服务器地址", paramType = "query", dataTypeClass = MongodbBackupServerDto.class)
    })
    @PostMapping("/clusters/{clusterId}/mongodb/backup")
    public BaseResult backupServer(@PathVariable("clusterId") String clusterId,
                                   @RequestPart("s3OpLogCertificateFile") MultipartFile s3OpLogCertificateFile,
                                   @RequestPart("s3CertificateFile") MultipartFile s3CertificateFile,
                                   @RequestPart MongodbBackupServerDto mongodbBackupServerDto){
        mongodbBackupServerDto.setClusterId(clusterId);
        // 处理证书文件
        if (!s3OpLogCertificateFile.isEmpty()) {
            try {
                String originalFilename = s3OpLogCertificateFile.getOriginalFilename();
                byte[] fileContent = s3OpLogCertificateFile.getBytes();
                // 示例保存逻辑，可以根据需求修改
                mongodbBackupServerDto.getS3OpLogBackup().setPemName(originalFilename);
                mongodbBackupServerDto.getS3OpLogBackup().setPemContent(new String(fileContent));
            } catch (Exception e) {
                // todo
            }
        }

        // 处理证书文件
        if (!s3CertificateFile.isEmpty()) {
            try {
                String originalFilename = s3CertificateFile.getOriginalFilename();
                byte[] fileContent = s3CertificateFile.getBytes();
                // 示例保存逻辑，可以根据需求修改
                mongodbBackupServerDto.getS3Backup().setPemName(originalFilename);
                mongodbBackupServerDto.getS3Backup().setPemContent(new String(fileContent));
            } catch (Exception e) {
                // todo
            }
        }


        mongodbBackupService.updateBackupServer(mongodbBackupServerDto);
        return BaseResult.ok();
    }

    @ApiOperation(value = "查询mongodb备份服务器地址", notes = "查询mongodb备份服务器地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @GetMapping("/clusters/{clusterId}/mongodb/backup")
    public BaseResult backupServer(@PathVariable("clusterId") String clusterId){
        return BaseResult.ok(mongodbBackupService.getBackupServer(clusterId));
    }


    @ApiOperation(value = "开启mongodb周期备份", notes = "查询mongodb备份服务器地址")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class)
    })
    @PostMapping("/clusters/{clusterId}/namespaces/{namespace}/mongodb/{name}/back")
    public BaseResult backupServer(@PathVariable("clusterId") String clusterId,
                                   @PathVariable("namespace") String namespace,
                                   @PathVariable("name") String name,
                                   @RequestBody MongodbScheduleBackupDto mongodbScheduleBackupDto) {
        mongodbScheduleBackupDto.setClusterId(clusterId).setNamespace(namespace).setName(name);
        mongodbBackupService.enableScheduleBackup(mongodbScheduleBackupDto);
        return BaseResult.ok();
    }




}
