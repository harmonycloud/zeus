package com.middleware.zeus.integration.cluster.bean;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author xutianhong
 * @Date 2023/1/10 9:57 上午
 */
@Accessors(chain = true)
@Data
public class MaintenancePvc {

    private String pvc;

    private String namespace;

    private String targetRequestSize;

    private String rollBackRequestSize;

    private String pod;

}
