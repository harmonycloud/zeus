package com.middleware.zeus.common.enums;

import io.swagger.annotations.ApiModel;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2023/5/6 4:14 下午
 */
@ApiModel(description = "告警对象枚举")
public enum AlertTargetEnum {

    platform("platform", "平台系统监控", "system"),
    logstash("logCollect", "日志采集", "system"),
    elasticsearch("elasticsearch", "日志组件", "system"),
    minio("minio", "备份存储", "system"),
    middlewareController("middlewareController", "中间件控制器", "system"),
    middlewareBackupController("middlewareBackupController", "备份控制器", "system"),
    middlewareScheduler("middlewareScheduler", "扩展调度器", "system"),
    lvmController("lvmController", "LVM控制器", "cluster"),
    localPathController("localPathController", "Local-Path控制器", "cluster"),
    grafana("grafana", "监控面板", "cluster"),
    prometheus("prometheus", "数据监控", "cluster"),
    alertmanager("alertmanager", "监控告警", "cluster"),
    netWork("network", "网络监控", "cluster"),
    cluster("cluster", "集群监控", "cluster"),
    ;

    private final String name;
    private final String aliasName;

    private final String type;

    private static final Map<String, AlertTargetEnum> map = new HashMap<>();

    static {
        for (AlertTargetEnum a : AlertTargetEnum.values()){
            map.put(a.name, a);
        }
    }

    AlertTargetEnum(String name, String aliasName, String type){
        this.name = name;
        this.aliasName = aliasName;
        this.type = type;
    }

    public String getName() {
        return name;
    }

    public String getAliasName() {
        return aliasName;
    }

    public String getType() {
        return type;
    }

    public static AlertTargetEnum getByName(String name){
        return map.get(name);
    }

}
