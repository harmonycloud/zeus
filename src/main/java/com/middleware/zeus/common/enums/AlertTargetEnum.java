package com.middleware.zeus.common.enums;

import java.util.HashMap;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2023/5/6 4:14 下午
 */
public enum AlertTargetEnum {

    platform("platform", "平台系统告警"),
    logstash("logCollect", "日志采集"),
    elasticsearch("elasticsearch", "日志组件"),
    grafana("grafana", "监控面板"),
    prometheus("prometheus", "数据监控"),
    alertmanager("alertmanager", "监控告警"),
    minio("minio", "备份存储"),
    middlewareController("middlewareController", "中间件控制器"),
    middlewareScheduler("middlewareScheduler", "扩展调度器"),
    lvmController("lvmController", "LVM控制器"),
    localPathController("localPathController", "Local-Path控制器"),
    cluster("cluster", "集群监控"),
    ;

    private final String name;
    private final String aliasName;

    private static final Map<String, String> map = new HashMap<>();

    static {
        for (AlertTargetEnum a : AlertTargetEnum.values()){
            map.put(a.name, a.aliasName);
        }
    }

    AlertTargetEnum(String name, String aliasName){
        this.name = name;
        this.aliasName = aliasName;
    }

    public String getName() {
        return name;
    }

    public String getAliasName() {
        return aliasName;
    }

    public static String getByName(String name){
        return map.get(name);
    }

}
