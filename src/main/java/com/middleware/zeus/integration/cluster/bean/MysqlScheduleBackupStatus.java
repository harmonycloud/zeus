package com.middleware.zeus.integration.cluster.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/6 9:18 上午
 */
@Data
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class MysqlScheduleBackupStatus {

    private String lastBackupFileName;

    private String lastBackupMessage;

    private String lastBackupName;

    private String lastBackupPhase;

    private String lastBackupTime;

}
