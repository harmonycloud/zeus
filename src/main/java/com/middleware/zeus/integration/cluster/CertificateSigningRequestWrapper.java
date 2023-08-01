package com.middleware.zeus.integration.cluster;

import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.certificates.v1.CertificateSigningRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author xutianhong
 * @Date 2023/7/24 5:17 下午
 */
@Slf4j
@Component
public class CertificateSigningRequestWrapper {

    //todo CertificateSigningRequest资源对象在1.19 从 v1beta1 变为 v1
    public void create(String clusterId, CertificateSigningRequest csr){
        K8sClient.getClient(clusterId).certificates().v1().certificateSigningRequests().resource(csr).create();
    }

    public CertificateSigningRequest get(String clusterId, String name){
        return K8sClient.getClient(clusterId).certificates().v1().certificateSigningRequests().withName(name).get();
    }

    public void approve(String clusterId, String name){
/*        CertificateSigningRequestCondition csrCondition = new CertificateSigningRequestConditionBuilder()
                .withType("Approved")
                .withStatus("True")
                .withReason("ApprovedViaRESTApi")
                .withMessage("Approved by REST API /approval endpoint.")
                .build();*/
        K8sClient.getClient(clusterId).certificates().v1().certificateSigningRequests().withName(name).approve();
    }

}
