package com.middleware.zeus.service.k8s.impl;

import com.middleware.zeus.integration.cluster.CertificateSigningRequestWrapper;
import com.middleware.zeus.service.k8s.CertificateSigningRequestService;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.certificates.v1.CertificateSigningRequest;
import io.fabric8.kubernetes.api.model.certificates.v1.CertificateSigningRequestSpec;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.units.qual.A;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collections;

/**
 * @author xutianhong
 * @Date 2023/7/24 5:29 下午
 */
@Service
@Slf4j
public class CertificateSigningRequestServiceImpl implements CertificateSigningRequestService {

    @Autowired
    private CertificateSigningRequestWrapper certificateSigningRequestWrapper;

    @Override
    public void create(String clusterId, String username, String csrStr, Integer expiredSeconds) {
        CertificateSigningRequest csr = new CertificateSigningRequest();

        ObjectMeta meta = new ObjectMeta();
        meta.setName(username);

        CertificateSigningRequestSpec spec =  new CertificateSigningRequestSpec();
        spec.setRequest(csrStr);
        if (expiredSeconds == null){
            expiredSeconds = 864000000;
        }
        spec.setExpirationSeconds(expiredSeconds);
        spec.setUsages(Collections.singletonList("client auth"));
        spec.setSignerName("kubernetes.io/kube-apiserver-client");

        csr.setMetadata(meta);
        csr.setSpec(spec);

        certificateSigningRequestWrapper.create(clusterId, csr);
    }

    @Override
    public CertificateSigningRequest get(String clusterId, String name) {
        CertificateSigningRequest certificateSigningRequest = certificateSigningRequestWrapper.get(clusterId, name);
        if (certificateSigningRequest == null){
            return null;
        }
        return certificateSigningRequest;
    }

    @Override
    public void approve(String clusterId, String username) {
        certificateSigningRequestWrapper.approve(clusterId, username);
    }

    @Override
    public String generateCertificate(String clusterId, String username, String csrStr, Integer expiredSeconds) {
        CertificateSigningRequest csr = this.get(clusterId, username);
        if (csr == null){
            // 创建CertificateSigningRequest
            this.create(clusterId, username, csrStr, expiredSeconds);
            // 同意CertificateSigningRequest
            this.approve(clusterId, username);
            // 等待1s
            try {
                Thread.sleep(1000);
            } catch (Exception ignored){
            }
            // 重新获取csr
            csr = this.get(clusterId, username);
        }
        // 获取certificate内容
        if (csr.getStatus() != null && StringUtils.isNotEmpty(csr.getStatus().getCertificate())){
            return csr.getStatus().getCertificate();
        }
        return null;
    }
}
