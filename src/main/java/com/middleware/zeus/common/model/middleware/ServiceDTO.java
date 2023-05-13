package com.middleware.zeus.common.model.middleware;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * @author tangtx
 * @date 4/1/21 4:40 PM
 */
@Accessors(chain = true)
@Data
@NoArgsConstructor
public class ServiceDTO {
    private String serviceName;
    private String servicePort;
    private String exposePort;
    private String targetPort;

    private String oldServiceName;
    private String oldServicePort;
    private String oldExposePort;
}
