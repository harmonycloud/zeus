package com.middleware.zeus.common.model;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @author yushuaikang
 * @date 2022/3/31 上午11:39
 */
@Data
public class RedisAccessInfo {

    private String clusterId;

    private String namespace;

    private String middlewareName;

    /**
     * 是否是对外服务
     */
    private boolean openService;

    private String mode;

    private String address;

    private String password;

    /**
     * 集群模式
     */
    private List<Map<String,Integer>> hostAndPort;
    private String masterName;

    /**
     * 哨兵模式
     */
    private String host;
    private String port;
}
