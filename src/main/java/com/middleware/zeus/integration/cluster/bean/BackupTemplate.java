package com.middleware.zeus.integration.cluster.bean;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/2 2:55 下午
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Accessors(chain = true)
public class BackupTemplate {

    private String clusterName;

    private BackupStorageProvider storageProvider;

}
