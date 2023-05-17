package com.middleware.zeus.common.enums;

/**
 * @author xutianhong
 * @Date 2021/10/29 3:29 下午
 */
public enum ComponentsEnum {

    ALERTMANAGER("alertmanager"),
    PROMETHEUS("prometheus"),
    LOGGING("logging"),
    MINIO("minio"),
    GRAFANA("grafana"),
    LOCAL_PATH("local-path"),
    MIDDLEWARE_CONTROLLER("middleware-controller"),
    MIDDLEWAREBACKUP_CONTROLLER("middlewarebackup-controller"),
    MIDDLEWARE_SCHEDULER("middleware-scheduler"),
    LVM("lvm");

    private String name;

    ComponentsEnum(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }
}
