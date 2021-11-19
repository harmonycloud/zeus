package com.harmonycloud.zeus.integration.cluster.bean;

import lombok.Data;

/**
 * @author liyinlong
 * @since 2021/11/18 10:18 上午
 */
@Data
public class RestoreObject {

    /**
     * pod名称
     */
    private String pod;

    /**
     * pod的pvc
     */
    private String pvc;

    /**
     * 恢复来源，来自备份记录对象
     */
    private String volumeSnapshot;

    public RestoreObject() {
    }

    public RestoreObject(String pod, String pvc, String volumeSnapshot) {
        this.pod = pod;
        this.pvc = pvc;
        this.volumeSnapshot = volumeSnapshot;
    }
}
