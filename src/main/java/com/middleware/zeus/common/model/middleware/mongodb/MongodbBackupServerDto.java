package com.middleware.zeus.common.model.middleware.mongodb;

import com.middleware.zeus.integration.cluster.bean.mongodb.OpsManager;
import com.middleware.zeus.integration.cluster.bean.mongodb.OpsManagerSpec;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.web.multipart.MultipartFile;

/**
 * @author xutianhong
 * @Date 2025/7/31 10:40
 */
@Data
@Accessors(chain = true)
@ApiModel("mongodb备份服务器")
@NoArgsConstructor
public class MongodbBackupServerDto {

    @ApiModelProperty("集群")
    private String clusterId;

    @ApiModelProperty("s3OpLog备份")
    private BackupSerer s3OpLogBackup;

    @ApiModelProperty("数据备份")
    private BackupSerer s3Backup;

    @Data
    @Accessors(chain = true)
    public static class BackupSerer {

        @ApiModelProperty("证书名称")
        private String pemName;

        @ApiModelProperty("证书内容")
        private String pemContent;

        @ApiModelProperty("协议")
        private String protocol;

        @ApiModelProperty("地址")
        private String url;

        @ApiModelProperty("端口")
        private Integer port;

        @ApiModelProperty("备份位置")
        private String bucket;

        @ApiModelProperty("用户名")
        private String username;

        @ApiModelProperty("密码")
        private String password;

        public BackupSerer(OpsManagerSpec.Backup.Store store) {
            String[] urls = store.getS3BucketEndpoint().split("://");
            this.protocol = (urls[0]);
            if (urls[1].contains(":")) {
                this.url = urls[1].split(":")[0];
                this.port = Integer.valueOf(urls[1].split(":")[1]);
            } else {
                this.url = urls[1];
            }
            this.bucket = store.getS3BucketName();
        }
    }

}
