package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/2 2:40 下午
 */
@Data
@Accessors(chain = true)
public class BackupStorageProvider {

    private Minio minio;

}
