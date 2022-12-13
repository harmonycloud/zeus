package com.harmonycloud.zeus.service.components.impl;

import com.middleware.caas.common.enums.ComponentsEnum;
import com.middleware.caas.common.enums.DictEnum;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.ClusterComponentsDto;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.service.components.AbstractBaseOperator;
import com.harmonycloud.zeus.service.components.api.LVMService;
import com.harmonycloud.zeus.service.k8s.ClusterComponentService;
import com.harmonycloud.zeus.util.AssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.middleware.caas.common.constants.CommonConstant.ALREADY_EXISTED;

/**
 * @author xutianhong
 * @Date 2021/12/8 2:10 下午
 */
@Slf4j
@Service
@Operator(paramTypes4One = String.class)
public class LVMServiceImpl extends AbstractBaseOperator implements LVMService {

    private static final Map<String, String> size = new ConcurrentHashMap<>();

    @Autowired
    private ClusterComponentService clusterComponentService;

    @Override
    public boolean support(String name) {
        return ComponentsEnum.LVM.getName().equals(name);
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        helmChartService.uninstall(cluster, "middleware-operator", ComponentsEnum.LVM.getName());
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {
        try {
            helmChartService.installComponents(ComponentsEnum.LVM.getName(), "middleware-operator", setValues,
                    componentsPath + File.separator + "lvm-csi-plugin", cluster);
        } catch (Exception e){
            if (StringUtils.isNotEmpty(e.getMessage()) && e.getMessage().contains(ALREADY_EXISTED)) {
                throw new BusinessException(ErrorMessage.LVM_ALREADY_EXISTED);
            } else {
                throw e;
            }
        }
    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        String url = getMinioUrl(cluster);
        AssertUtil.notBlank(clusterComponentsDto.getVgName(), DictEnum.ACCESS_KEY_ID);
        AssertUtil.notBlank(clusterComponentsDto.getVgName(), DictEnum.SIZE);
        size.put("size", clusterComponentsDto.getSize());
        return "image.repository=" + repository +
                ",storage.vgName=" + clusterComponentsDto.getVgName() +
                ",storage.size=" + clusterComponentsDto.getSize() +
                ",volumeSnapshotClass.bucket=velero" +
                ",volumeSnapshotClass.url=" + url;
    }

    @Override
    public void initAddress(ClusterComponentsDto clusterComponentsDto, MiddlewareClusterDTO cluster){

    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        return podService.list(clusterId, "middleware-operator", ComponentsEnum.LVM.getName());
    }

    public String getMinioUrl(MiddlewareClusterDTO cluster) {
        ClusterComponentsDto clusterComponentsDto = clusterComponentService.get(cluster.getId(), ComponentsEnum.MINIO.getName());
        if (StringUtils.isEmpty(clusterComponentsDto.getHost())){
            log.error("未安装minio，使用默认minio地址");
            return "http://" + cluster.getHost() + ":31909";
        }else {
            return clusterComponentsDto.getAddress();
        }
    }

}
