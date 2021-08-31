package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.ClusterCert;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;

/**
 * @author dengyulong
 * @date 2021/03/30
 */
public interface ClusterCertService {

    /**
     * 获取证书的cm名称
     *
     * @param clusterId 集群id
     * @return
     */
    static String getCertCmName(String clusterId) {
        return "admin-conifg--" + clusterId;
    }

    /**
     * 保存证书
     * @param cluster 集群信息
     *
     */
    void saveCert(MiddlewareClusterDTO cluster);

    /**
     * 根据证书生成token
     *
     * @param cluster 集群信息
     */
    void generateTokenByCert(MiddlewareClusterDTO cluster);

    /**
     * 获取kube config文件的路径
     *
     * @param clusterId 集群id
     * @return
     */
    String getKubeConfigFilePath(String clusterId);

    /**
     * 根据admin.conf内容设置证书信息
     *
     * @param cert 集群证书
     */
    void setCertByAdminConf(ClusterCert cert);

}
