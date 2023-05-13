package com.middleware.zeus.common.model;

import lombok.Data;

/**
 * @author liyinlong
 * @since 2022/6/15 2:25 下午
 */
@Data
public class ClusterDTO {

    private String id;

    private String name;

    private String aliasName;

    private String apiServerUrl;

    private String host;

    private String apiCa;

    private String apiCrt;

    private String apiKey;

    private String compAddress;

    private String prometheusPort;

    private String esPort;

    private String esUser;

    private String esPwd;

    private String protocol;

    private int port;

    private Boolean isEnable;
}
