package com.middleware.zeus.service.k8s.skyviewimpl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.ClusterDTO;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.middleware.Registry;
import com.middleware.zeus.annotation.Skyview;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.service.k8s.abstractService.AbstractClusterService;
import com.middleware.zeus.skyview.v2.service.V2ClusterService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author liyinlong
 * @since 2022/6/17 11:02 上午
 */
@Slf4j
@Service
@Skyview(target = "skyview2")
public class Skyview2ClusterServiceImpl extends AbstractClusterService implements ClusterService {

    @Autowired
    private V2ClusterService v2ClusterService;

    @Override
    protected List<MiddlewareClusterDTO> baseListCluster() {
        return v2ClusterService.list();
    }

    @Override
    public MiddlewareClusterDTO detail(String clusterId) {
        return v2ClusterService.get(clusterId);
    }

    @Override
    public void addCluster(MiddlewareClusterDTO cluster) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void updateCluster(MiddlewareClusterDTO cluster) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void update(MiddlewareClusterDTO cluster) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public void removeCluster(String clusterId) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public String getClusterJoinCommand(String clusterName, String apiAddress, String userToken, Registry registry) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public BaseResult quickAdd(MultipartFile adminConf, String name, Registry registry) {
        throw new BusinessException(ErrorMessage.NO_AUTHORITY_WITH_EXTERNAL_SERVICE);
    }

    @Override
    public boolean checkIfExists(String clusterId) {
        List<MiddlewareClusterDTO> clusterList = baseListCluster();
        return clusterList.stream().anyMatch(cluster -> cluster.getId().equals(clusterId));
    }

    @Override
    public String convertToSkyviewClusterId(String skyviewClusterId) {
        return null;
    }

    @Override
    public String convertToZeusClusterId(String zeusClusterId) {
        return null;
    }

    @Override
    public ClusterDTO findBySkyviewClusterId(String skyviewClusterId) {
        return null;
    }

}
