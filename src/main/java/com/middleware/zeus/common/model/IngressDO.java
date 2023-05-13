package com.middleware.zeus.common.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * @author chenbilong
 * @since 2019-05-24 10:11
 */
@Data
@NoArgsConstructor
public class IngressDO {

    private String name;
    private String namespace;
    private String namespaceNickname;
    private String clusterId;
    private String clusterNickname;
    private String loadbalancerName;
    private String loadbalancerNickName;
    private List<IngressService> rules;

    private String serviceName;
    private String containerPort;
    private String exposePort;
    private String protocol;
    Map<String, List<String>> address;

}
