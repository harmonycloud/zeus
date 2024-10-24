package com.middleware.zeus.common.model;

import lombok.Data;

/**
 * @author wangpenglei
 * @data 2024/10/23 10:26
 */
@Data
public class MysqlDRAccessInfo {

    private MysqlAccessInfo source;

    private MysqlAccessInfo disasterRecovery;

}
