package com.middleware.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/2 2:41 下午
 */
@Data
@Accessors(chain = true)
public class Minio {

    private String name;

    private String endpoint;

    private String bucketName;

    private String backupFileName;

    private String accessKeyId;

    private String secretAccessKey;

    private Integer capacity;

}
