package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/6 4:47 下午
 */
@Data
@Accessors(chain = true)
public class BackupStatus {

    private String backupFileName;

    private String backupTime;

    private String message;

    private String phase;
}
