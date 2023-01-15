package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.Date;

/**
 * @author wangpenglei
 * @Date 2023/1/15 下午8:12
 **/
@Accessors(chain = true)
@Data
public class MigrateInfo {
    private String status;
    private String reason;
    private Date migrateTimestamp;
}
