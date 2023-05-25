package com.middleware.zeus.service.components.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.common.enums.ComponentsEnum;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.model.ClusterComponentsDto;
import com.middleware.zeus.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.zeus.common.model.middleware.PodInfo;
import com.middleware.caas.filters.user.CurrentUserRepository;
import com.middleware.zeus.annotation.Operator;
import com.middleware.zeus.bean.BeanSystemConfig;
import com.middleware.zeus.dao.BeanSystemConfigMapper;
import com.middleware.zeus.service.components.AbstractBaseOperator;
import com.middleware.zeus.service.components.api.AlertManagerService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import static com.middleware.zeus.common.constants.CommonConstant.SIMPLE;

import java.io.File;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2021/10/29 2:47 下午
 */
@Service
@Operator(paramTypes4One = String.class)
@Slf4j
public class AlertManagerServiceImpl extends AbstractBaseOperator implements AlertManagerService {

    @Value("${system.alert.silent:1h}")
    private String silentTime;

    @Autowired
    private BeanSystemConfigMapper beanSystemConfigMapper;

    @Override
    public boolean support(String name) {
        return ComponentsEnum.ALERTMANAGER.getName().equals(name);
    }

    @Override
    public void deploy(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        if (namespaceService.list(cluster.getId()).stream().noneMatch(ns -> "monitoring".equals(ns.getName()))){
            //创建分区
            namespaceService.save(cluster.getId(), "monitoring", null, null);
        }
        //发布alertManager
        try {
            super.deploy(cluster, clusterComponentsDto);
        } catch (Exception e){
            if (StringUtils.isNotEmpty(e.getMessage()) && e.getMessage().contains("no matches for kind")) {
                log.error("识别部分资源类型失败", e);
                throw new BusinessException(ErrorMessage.CRD_NOT_EXISTED);
            } else {
                throw e;
            }
        }
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        //uninstall
        if (status != 1){
            helmChartService.uninstall(cluster, "monitoring", ComponentsEnum.ALERTMANAGER.getName());
        }
    }

    @Override
    public String getValues(String repository, MiddlewareClusterDTO cluster,
        ClusterComponentsDto clusterComponentsDto) {
        String setValues = "alertmanager.alertmanagerSpec.image.repository=" + repository + "/alertmanager";
        // 设置平台后端访问地址
        if (StringUtils.isNoneEmpty(clusterComponentsDto.getPlatformProtocol(),
            clusterComponentsDto.getPlatformHost())) {
            setValues = setValues + ",alertmanager.clusterHost=" + clusterComponentsDto.getPlatformProtocol() + "://"
                + clusterComponentsDto.getPlatformHost()
                + (clusterComponentsDto.getPlatformPort() == null ? "" : ":" + clusterComponentsDto.getPlatformPort());
        }
        if (SIMPLE.equals(clusterComponentsDto.getType())) {
            setValues = setValues + ",alertmanager.alertmanagerSpec.replicas=1";
        } else {
            setValues = setValues + ",alertmanager.alertmanagerSpec.replicas=3";
        }
        return setValues;
    }

    @Override
    public void install(String setValues, MiddlewareClusterDTO cluster){
        helmChartService.installComponents(ComponentsEnum.ALERTMANAGER.getName(), "monitoring", setValues,
                componentsPath + File.separator + "alertmanager", cluster);
    }

    @Override
    public void initAddress(ClusterComponentsDto clusterComponentsDto, MiddlewareClusterDTO cluster){
        if (StringUtils.isEmpty(clusterComponentsDto.getProtocol())){
            clusterComponentsDto.setProtocol("http");
        }
        if (StringUtils.isEmpty(clusterComponentsDto.getHost())){
            clusterComponentsDto.setHost(cluster.getHost());
        }
        if (StringUtils.isEmpty(clusterComponentsDto.getPort())){
            clusterComponentsDto.setPort("31902");
        }
    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        Map<String, String> labels = new HashMap<>();
        labels.put("app.kubernetes.io/name", ComponentsEnum.ALERTMANAGER.getName());
        return podService.list(clusterId, "monitoring", labels);
    }

    @Override
    public void setStatus(ClusterComponentsDto clusterComponentsDto) {
        if (StringUtils.isAnyEmpty(clusterComponentsDto.getProtocol(), clusterComponentsDto.getHost(),
            clusterComponentsDto.getPort())) {
            clusterComponentsDto.setStatus(7);
        }
    }

    @Override
    public void record2SystemConfig(ClusterComponentsDto clusterComponentsDto) {
        QueryWrapper<BeanSystemConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("config_name", "Alertmanager_SilentTime");
        List<BeanSystemConfig> beanSystemConfigs = beanSystemConfigMapper.selectList(wrapper);
        BeanSystemConfig beanSystemConfig;
        if (StringUtils.isEmpty(clusterComponentsDto.getSilentTime())){
            clusterComponentsDto.setSilentTime(silentTime);
        }
        if (!CollectionUtils.isEmpty(beanSystemConfigs)) {
            beanSystemConfig = beanSystemConfigs.get(0);
            beanSystemConfig.setConfigValue(clusterComponentsDto.getSilentTime());
            beanSystemConfig.setUpdateUser(CurrentUserRepository.getUser().getUsername());
            beanSystemConfigMapper.update(beanSystemConfig, wrapper);
        } else {
            beanSystemConfig = new BeanSystemConfig();
            beanSystemConfig.setConfigName("Alertmanager_SilentTime");
            beanSystemConfig.setConfigValue(clusterComponentsDto.getSilentTime());
            beanSystemConfig.setCreateTime(LocalDateTime.now());
            beanSystemConfig.setCreateUser(CurrentUserRepository.getUser().getUsername());
            beanSystemConfigMapper.insert(beanSystemConfig);
        }
    }

    @Override
    public void readSystemConfig(ClusterComponentsDto clusterComponentsDto) {
        QueryWrapper<BeanSystemConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("config_name", "Alertmanager_SilentTime");
        List<BeanSystemConfig> beanSystemConfigs = beanSystemConfigMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(beanSystemConfigs)) {
            clusterComponentsDto.setSilentTime(beanSystemConfigs.get(0).getConfigValue());
        }
    }
}
