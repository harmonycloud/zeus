package com.middleware.zeus.common.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author LanChao
 * @date 2021-01-15 09:18:43
 */
@Accessors(chain = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceIngress {

    private String clusterId;
    private String namespace;
    private String serviceName;
    private String type;
}
