package com.middleware.zeus.common.model;

import com.skyview.language.annotations.RegexTranslate;
import lombok.Data;

/**
 * @author liyinlong
 * @since 2022/3/30 10:12 上午
 */
@Data
public class MysqlAccessInfo {

    private String clusterId;

    private String namespace;

    private String middlewareName;

    /**
     * 是否是对外服务
     */
    private boolean openService;

    @RegexTranslate(staticGroupName = "mysql_access_info")
    private String address;

    private String host;

    private String port;

    private String username;

    private String password;

    private Boolean withInCluster;

}
