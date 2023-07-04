package com.middleware.zeus.common.model.middleware;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author chenbilong
 * @since 2019-05-24 10:11
 */
@Accessors(chain = true)
@Data
@NoArgsConstructor
public class IngressDTO {

    /**
     * base info
     */
    private String name;
    private String namespace;
    private String namespaceNickname;
    private String clusterId;
    private String clusterNickname;
    private String ingressClassName;

    /**
     * base routing info
     */
    private String exposeType;
    private String protocol;
    private String httpExposePort;

    /**
     * middleware info
     */
    private String middlewareName;
    private String middlewareNickName;
    private String middlewareType;
    private String middlewareOfficialName;
    private String middlewareMode;
    private String servicePurpose;
    private String chartVersion;
    private String imagePath;
    private Map<String, String> labels;


    /**
     * http routing info
     */
    private List<IngressRuleDTO> rules;

    /**
     * tcp / nodePort routing info
     */
    private String exposeIP;
    private List<ServiceDTO> serviceList;

    /**
     * 是否是灾备实例(mysql专有字段)
     */
    private Boolean isDisasterRecovery;

    /**
     * 对外访问端口
     */
    private String exposePort;

    /**
     * 服务端口
     */
    private String servicePort;

    /**
     * 创建时间
     */
    private String createTime;

    /**
     * k8s资源ownerReferences
     */
    private Object ownerReferences;

    /**
     * ingress列表
     */
    private Set<String> ingressIpSet;

    /**
     * 四层或七层
     */
    private Integer networkModel;

    /**
     * ingress vip
     */
    private String address;

    /**
     * 是否开启集群外访问
     */
    private Boolean externalEnable;
    @ApiModelProperty("跳过端口冲突")
    private Boolean skipPortConflict;

    public IngressDTO(String clusterId, String namespace, String middlewareType, String middlewareName) {
        this.namespace = namespace;
        this.clusterId = clusterId;
        this.middlewareName = middlewareName;
        this.middlewareType = middlewareType;
    }

}
