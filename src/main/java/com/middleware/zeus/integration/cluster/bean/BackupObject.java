package com.middleware.zeus.integration.cluster.bean;

import lombok.Data;

import java.util.List;

/**
 * 备份信息
 */
@Data
public class BackupObject {
    /**
     * pod名称
     */
    private String pod;

    /**
     * pod角色
     */
    private String podRole;

    /**
     * pod绑定的pvc
     */
    private List<String> pvcs;

    public BackupObject() {
    }

    public BackupObject(String pod, List<String> pvcs) {
        this.pod = pod;
        this.pvcs = pvcs;
    }

    public BackupObject(String pod, String podRole, List<String> pvcs) {
        this.pod = pod;
        this.podRole = podRole;
        this.pvcs = pvcs;
    }
}