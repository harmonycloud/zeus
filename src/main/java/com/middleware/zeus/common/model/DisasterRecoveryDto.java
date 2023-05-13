package com.middleware.zeus.common.model;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @auther wangpenglei
 * @date 2023/3/23 11:31
 */
@Accessors(chain = true)
@Data
public class DisasterRecoveryDto {
    private Boolean isMaster;
    private DisasterRecoveryInfo local;
    private DisasterRecoveryInfo relation;
    private Date lastSwitchTime;
    private Date lastUpdateTime;
    private String replicatePhase;
}
