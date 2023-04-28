package com.middleware.zeus.integration.cluster.bean;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2021/4/6 9:18 上午
 */
@AllArgsConstructor
@NoArgsConstructor
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
