package com.harmonycloud.zeus.service.middleware.impl;import java.io.File;import java.io.IOException;import java.util.Comparator;import java.util.List;import java.util.Map;import java.util.stream.Collectors;import com.harmonycloud.caas.common.model.middleware.Middleware;import com.harmonycloud.zeus.service.middleware.*;import org.apache.commons.lang3.StringUtils;import org.springframework.beans.factory.annotation.Autowired;import org.springframework.beans.factory.annotation.Value;import org.springframework.stereotype.Service;import org.springframework.transaction.annotation.Transactional;import org.springframework.util.CollectionUtils;import org.springframework.util.ObjectUtils;import org.springframework.web.multipart.MultipartFile;import com.harmonycloud.caas.common.enums.ErrorMessage;import com.harmonycloud.caas.common.exception.BusinessException;import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;import com.harmonycloud.caas.common.model.registry.HelmChartFile;import com.harmonycloud.tool.file.FileUtil;import com.harmonycloud.zeus.bean.BeanClusterMiddlewareInfo;import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;import com.harmonycloud.zeus.service.AbstractBaseService;import com.harmonycloud.zeus.service.k8s.ClusterService;import com.harmonycloud.zeus.service.registry.HelmChartService;import lombok.extern.slf4j.Slf4j;/** * @Author: zack chen * @Date: 2021/5/14 11:00 上午 */@Slf4j@Service@Transactional(rollbackFor = {RuntimeException.class})public class MiddlewareManagerServiceImpl extends AbstractBaseService implements MiddlewareManagerService {    @Value("${system.upload.path:/usr/local/zeus-pv/upload}")    private String uploadPath;    @Autowired    private HelmChartService helmChartService;    @Autowired    private MiddlewareAlertsService middlewareAlertsService;    @Autowired    private MiddlewareCustomConfigService middlewareCustomConfigService;    @Autowired    private MiddlewareInfoService middlewareInfoService;    @Autowired    private ClusterMiddlewareInfoServiceImpl clusterMiddlewareInfoService;    @Autowired    private ClusterService clusterService;    @Autowired    private MiddlewareService middlewareService;    /**     * 中间件上架     * @param clusterId     * @param fileIn     */    @Override    public void upload(String clusterId, MultipartFile fileIn) {        File tempStoredFile = null;        File tarFileDir = null;        try {            long tempId = System.currentTimeMillis();            // 临时保存到本地            String fileName = fileIn.getOriginalFilename();            String tempDirPath = uploadPath + File.separator + "temp" + File.separator + tempId;            tarFileDir = new File(tempDirPath);            if (!tarFileDir.exists() && !tarFileDir.mkdirs()) {                throw new BusinessException(ErrorMessage.CREATE_TEMPORARY_FILE_ERROR);            }            tempStoredFile = new File(tempDirPath + File.separator + fileName);            fileIn.transferTo(tempStoredFile);            // 解析包并入库信息.根据包名称版本进行更新            HelmChartFile helmChartFile = helmChartService.getHelmChartFromFile("", "", tempStoredFile);            // 校验该版本是否已存在            BeanMiddlewareInfo beanMiddlewareInfo = middlewareInfoService.get(helmChartFile.getChartName(), helmChartFile.getChartVersion());            if (!ObjectUtils.isEmpty(beanMiddlewareInfo)){                throw new BusinessException(ErrorMessage.HELM_CHART_EXIST);            }            // 更新告警规则和自定义配置至数据库            update2Mysql(helmChartFile);            // 将helm chart信息 存入数据库            middlewareInfoService.insert(helmChartFile, tempStoredFile);        } catch (IOException e) {            log.error("error when create temp file!", e);        } finally {            FileUtil.deleteFile(tarFileDir);        }    }    @Override    public void install(String clusterId, String chartName, String chartVersion, String type ) throws Exception {        HelmChartFile chartFile = helmChartService.getHelmChartFromMysql(chartName, chartVersion);        BeanClusterMiddlewareInfo clusterMwInfo = new BeanClusterMiddlewareInfo();        clusterMwInfo.setChartName(chartName);        clusterMwInfo.setChartVersion(chartVersion);        clusterMwInfo.setClusterId(clusterId);        //特殊处理无operator中间件        if (CollectionUtils.isEmpty(chartFile.getDependency())){            clusterMwInfo.setStatus(1);            clusterMiddlewareInfoService.update(clusterMwInfo);            return;        }        clusterMwInfo.setStatus(0);        clusterMiddlewareInfoService.update(clusterMwInfo);        helmChartService.createOperator(getFilePath(chartName, chartVersion), clusterId, chartFile, type);    }    @Override    public void delete(String clusterId, String chartName, String chartVersion) {        List<Middleware> middlewareList = middlewareService.simpleList(clusterId, null, chartName, null);        if (!CollectionUtils.isEmpty(middlewareList)){            throw new BusinessException(ErrorMessage.MIDDLEWARE_SERVICE_EXIST);        }        List<BeanMiddlewareInfo> mwInfoList = middlewareInfoService.listByType(chartName);        Map<String, BeanMiddlewareInfo> mwInfoMap =            mwInfoList.stream().collect(Collectors.toMap(BeanMiddlewareInfo::getChartVersion, info -> info));        if (StringUtils.isNotEmpty(mwInfoMap.get(chartVersion).getOperatorName())) {            helmChartService.uninstall(clusterService.findById(clusterId), "middleware-operator",                    mwInfoMap.get(chartVersion).getOperatorName());        }        clusterMiddlewareInfoService.delete(clusterId, chartName, chartVersion);        //重新写入未安装的最新中间件信息        mwInfoList.sort(Comparator.comparing(BeanMiddlewareInfo::getChartVersion));        BeanClusterMiddlewareInfo clusterMwInfo = new BeanClusterMiddlewareInfo();        clusterMwInfo.setClusterId(clusterId);        clusterMwInfo.setChartName(mwInfoList.get(0).getChartName());        clusterMwInfo.setChartVersion(mwInfoList.get(0).getChartVersion());        clusterMwInfo.setStatus(2);        clusterMiddlewareInfoService.insert(clusterMwInfo);    }    @Override    public void update(String clusterId, String chartName, String chartVersion) {        BeanClusterMiddlewareInfo clusterMwInfo = new BeanClusterMiddlewareInfo();        //特殊处理无operator中间件        BeanMiddlewareInfo mwInfo = middlewareInfoService.get(chartName, chartVersion);        if (StringUtils.isEmpty(mwInfo.getOperatorName())){            clusterMwInfo.setChartName(chartName);            clusterMwInfo.setChartVersion(chartVersion);            clusterMwInfo.setClusterId(clusterId);            clusterMwInfo.setStatus(1);            clusterMiddlewareInfoService.update(clusterMwInfo);            return;        }        // 更新状态为安装中        clusterMwInfo.setChartName(chartName);        clusterMwInfo.setChartVersion(chartVersion);        clusterMwInfo.setClusterId(clusterId);        clusterMwInfo.setStatus(0);        clusterMiddlewareInfoService.update(clusterMwInfo);        HelmChartFile chartFile = helmChartService.getHelmChartFromMysql(chartName, chartVersion);        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);        String setValues = "image.repository=" + cluster.getRegistry().getRegistryAddress() + "/" + cluster.getRegistry().getChartRepo();        helmChartService.installComponents(chartFile.getDependency().get("alias"), "middleware-operator", setValues,            getOperatorPath(chartName, chartVersion, chartFile), cluster);    }    /**     * 更新数据至数据库     */    public void update2Mysql(HelmChartFile helmChart) {        try {            middlewareAlertsService.updateAlerts2Mysql(helmChart);        } catch (Exception e) {            log.error("更新告警规则至数据库失败");        }        try {            middlewareCustomConfigService.updateConfig2MySQL(helmChart);        } catch (Exception e) {            log.error("更新自定义配置至数据库失败");        }    }    public String getFilePath(String chartName, String chartVersion){        return uploadPath + File.separator + "helmcharts" + File.separator + chartName + File.separator + chartVersion;    }        public String getOperatorPath(String chartName, String chartVersion, HelmChartFile chartFile) {        return getFilePath(chartName, chartVersion) + File.separator + chartFile.getTarFileName()            + chartFile.getDependency().get("repository").substring(8);    }}