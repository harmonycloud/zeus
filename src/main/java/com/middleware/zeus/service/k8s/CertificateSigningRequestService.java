package com.middleware.zeus.service.k8s;

import io.fabric8.kubernetes.api.model.certificates.v1.CertificateSigningRequest;

/**
 * @author xutianhong
 * @Date 2023/7/24 5:28 下午
 */
public interface CertificateSigningRequestService {

    /**
     * 创建csr
     * @param clusterId 集群id
     * @param username 用户名
     * @param csrStr csr内容
     * @param expiredSeconds 过期时间
     *
     */
    void create(String clusterId, String username, String csrStr, Integer expiredSeconds);

    /**
     * 获取csr
     * @param clusterId 集群id
     * @param name 名称
     *
     * @return CertificateSigningRequest
     */
    CertificateSigningRequest get(String clusterId, String name);

    /**
     * 同意csr申请
     * @param clusterId 集群id
     * @param username 用户名
     */
    void approve(String clusterId, String username);

    /**
     * 同意csr申请
     * @param clusterId 集群id
     * @param username 用户名
     * @param csrStr csr内容
     * @param expiredSeconds 过期时间
     * @return String
     */
    String generateCertificate(String clusterId, String username, String csrStr, Integer expiredSeconds);

}
