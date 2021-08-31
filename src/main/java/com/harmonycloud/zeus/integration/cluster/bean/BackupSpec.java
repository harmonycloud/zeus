package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/6 4:46 下午
 */
@Data
@Accessors(chain = true)
public class BackupSpec {

    private String clusterName;

    private BackupStorageProvider storageProvider;

}
