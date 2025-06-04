package com.middleware.zeus.service.k8s.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.zeus.annotation.Skyview;
import com.middleware.zeus.bean.BeanKubeConfig;
import com.middleware.zeus.bean.BeanMiddlewareCluster;
import com.middleware.zeus.bean.BeanMiddlewareInfo;
import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.constants.DateStyle;
import com.middleware.zeus.common.enums.DictEnum;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import com.middleware.zeus.common.exception.CaasRuntimeException;
import com.middleware.zeus.common.model.ClusterCert;
import com.middleware.zeus.common.model.ClusterComponentsDto;
import com.middleware.zeus.common.model.ClusterDTO;
import com.middleware.zeus.common.model.middleware.*;
import com.middleware.zeus.common.model.registry.HelmChartFile;
import com.middleware.zeus.dao.BeanKubeConfigMapper;
import com.middleware.zeus.dao.BeanMiddlewareClusterMapper;
import com.middleware.zeus.integration.cluster.bean.MiddlewareCluster;
import com.middleware.zeus.integration.cluster.bean.MiddlewareClusterInfo;
import com.middleware.zeus.integration.cluster.bean.MiddlewareClusterSpec;
import com.middleware.zeus.service.k8s.*;
import com.middleware.zeus.service.k8s.abstractService.AbstractClusterService;
import com.middleware.zeus.service.middleware.*;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.service.user.ProjectService;
import com.middleware.zeus.util.K8sClient;
import com.middleware.zeus.util.ThreadPoolExecutorFactory;
import com.middleware.zeus.util.YamlUtil;
import com.middleware.zeus.util.date.DateUtils;
import com.skyview.language.annotations.TranslateAfterResult;
import io.fabric8.kubernetes.api.model.ConfigMap;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.middleware.zeus.common.constants.NameConstant.*;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@Slf4j
@Service
@Skyview(target = "zeus")
public class ClusterServiceImpl extends AbstractClusterService implements ClusterService {

    @Value("${system.upload.path:/usr/local/zeus-pv/upload}")
    private String uploadPath;
    @Value("${k8s.default.dc:default}")
    private String dc;

    @Autowired
    private MiddlewareClusterService middlewareClusterService;
    @Autowired
    private ClusterCertService clusterCertService;
    @Autowired
    private K8sClient k8sClient;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private MiddlewareInfoService middlewareInfoService;
    @Autowired
    private ClusterComponentService clusterComponentService;
    @Autowired
    private IngressComponentService ingressComponentService;
    @Autowired
    private ImageRepositoryService imageRepositoryService;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private ActiveAreaService activeAreaService;
    @Autowired
    private MiddlewareCrTypeService middlewareCrTypeService;
    @Autowired
    private MiddlewareAlertsService middlewareAlertsService;
    @Autowired
    private BeanMiddlewareClusterMapper middlewareClusterMapper;
    @Autowired
    private BackupServerService backupServerService;
    @Autowired
    private ConfigMapService configMapService;
    @Autowired
    private GrafanaService grafanaService;
    @Autowired
    private BeanKubeConfigMapper kubeConfigMapper;
    @Autowired
    private RoleBindingService roleBindingService;
    @Autowired
    private ClusterRoleService clusterRoleService;

    @Value("${k8s.component.middleware:/usr/local/zeus-pv/middleware}")
    private String middlewarePath;

    @Override
    public List<MiddlewareClusterDTO> baseListCluster(){
        return middlewareClusterService.listClusterDtos();
    }

    @Override
    public MiddlewareClusterDTO detail(String clusterId) {
        List<MiddlewareCluster> middlewareClusterList = middlewareClusterService.listClusters(clusterId);
        if (CollectionUtils.isEmpty(middlewareClusterList)) {
            throw new BusinessException(ErrorMessage.CLUSTER_NOT_FOUND);
        }
        return convertMiddlewareClusterToDto(middlewareClusterList.get(0));
    }

    @Override
    public void addCluster(MiddlewareClusterDTO cluster) {
        // 校验集群基本信息参数
        if (cluster == null) {
            throw new IllegalArgumentException("cluster base info is null");
        }
        // 校验集群基本信息
        checkParams(cluster);
        // 校验集群是否已存在
        checkClusterExistent(cluster, false);
        cluster.setId(K8sClient.getClusterId(cluster));
        // 设置证书信息
        clusterCertService.setCertByAdminConf(cluster);
        try {
            // 先添加fabric8客户端，否则无法用fabric8调用APIServer
            k8sClient.addK8sClient(cluster, false);
            K8sClient.getClient(cluster.getId()).namespaces().withName(DEFAULT).get();
        } catch (CaasRuntimeException ignore) {
        } catch (Exception e) {
            log.error("集群：{}，校验基本信息异常", cluster.getName(), e);
            // 移除fabric8客户端
            K8sClient.removeClient(cluster.getId());
            throw new BusinessException(DictEnum.CLUSTER, cluster.getName(), ErrorMessage.AUTH_FAILED);
        }
        // 保存证书
        try {
            clusterCertService.saveCert(cluster);
        } catch (Exception e) {
            log.error("集群{}，保存证书异常", cluster.getId(), e);
        }
        // 保存集群
        MiddlewareCluster mw = convert(cluster);
        try {
            middlewareClusterService.create(cluster.getId(), mw);
            JSONObject attributes = new JSONObject();
            attributes.put(CREATE_TIME, new Date());
            cluster.setAttributes(attributes);
            CLUSTER_MAP.put(cluster.getId(), cluster);
        } catch (Exception e) {
            log.error("集群id：{}，添加集群异常", cluster.getId());
            throw new BusinessException(DictEnum.CLUSTER, cluster.getNickname(), ErrorMessage.ADD_FAIL);
        }
        // 将镜像仓库信息存进数据库
        if (cluster.getRegistry() != null && cluster.getRegistry().getAddress() != null) {
            insertMysqlImageRepository(cluster);
        }
        // 将chart包存进数据库
        initMiddlewareChart();
        // 判断middleware-operator分区是否存在，不存在则创建
        synchronized (this) {
            List<Namespace> namespaceList = namespaceService.list(cluster.getId(), false, "middleware-operator");
            // 检验分区是否存在
            if (CollectionUtils.isEmpty(namespaceList)) {
                Map<String, String> label = new HashMap<>();
                label.put("middleware", "middleware");
                namespaceService.save(cluster.getId(), "middleware-operator", label, null);
            }
        }
        // 创建clusterRole资源
        clusterRoleService.initClusterRole(cluster.getId());
    }

    @Override
    public void updateCluster(MiddlewareClusterDTO cluster) {
        // 校验集群基本信息参数
        if (StringUtils.isAnyEmpty(cluster.getNickname())) {
            throw new IllegalArgumentException("cluster nickname is null");
        }
        checkParams(cluster);

        // 校验集群基本信息
        // 校验集群是否已存在
        MiddlewareClusterDTO oldCluster = findById(cluster.getId());
        if (oldCluster == null) {
            throw new BusinessException(DictEnum.CLUSTER, cluster.getNickname(), ErrorMessage.NOT_EXIST);
        }
        checkClusterExistent(cluster, true);
        // 设置证书信息
        clusterCertService.setCertByAdminConf(cluster.getCert());
        k8sClient.updateK8sClient(cluster);

        // 只修改昵称，证书，ingress，制品服务，es
        oldCluster.setNickname(cluster.getNickname());
        oldCluster.setCert(cluster.getCert());
        oldCluster.setRegistry(cluster.getRegistry());
        oldCluster.setLogging(cluster.getLogging());
        oldCluster.setActiveActive(cluster.getActiveActive());

        update(oldCluster);
    }

    @Override
    public void update(MiddlewareClusterDTO cluster) {
        try {
            middlewareClusterService.update(cluster.getId(), convert(cluster));
            if (CLUSTER_MAP.containsKey(cluster.getId())) {
                CLUSTER_MAP.put(cluster.getId(), cluster);
            }
        } catch (Exception e) {
            log.error("集群{}的accessToken更新失败", cluster.getId());
            throw new BusinessException(DictEnum.CLUSTER, cluster.getNickname(), ErrorMessage.UPDATE_FAIL);
        }
    }

    private void checkParams(MiddlewareClusterDTO cluster) {
        if (cluster.getCert() == null || StringUtils.isEmpty(cluster.getCert().getCertificate())) {
            throw new IllegalArgumentException("cluster cert info is null");
        }
        if(StringUtils.isEmpty(cluster.getDcId())){
            cluster.setDcId(dc);
        }
    }

    @Override
    public void removeCluster(String clusterId) {
        if (!checkDelete(clusterId)) {
            throw new BusinessException(ErrorMessage.CLUSTER_NOT_EMPTY);
        }
        MiddlewareClusterDTO cluster = this.findById(clusterId);
        if (cluster == null) {
            return;
        }
        try {
            middlewareClusterService.delete(clusterId);
        } catch (Exception e) {
            log.error("集群id：{}，删除集群异常", clusterId, e);
            throw new BusinessException(DictEnum.CLUSTER, cluster.getNickname(), ErrorMessage.DELETE_FAIL);
        }
        CLUSTER_MAP.remove(clusterId);
        // 关联数据库信息删除
        bindResourceDelete(cluster);
    }

    public void bindResourceDelete(MiddlewareClusterDTO cluster) {
        // 删除集群组件信息
        clusterComponentService.delete(cluster.getId());
        // 删除ingress信息
        ingressComponentService.delete(cluster.getId());
        // 移除镜像仓库信息
        imageRepositoryService.removeImageRepository(cluster.getId());
        // 删除可用区初始化状态信息
        activeAreaService.delete(cluster.getId());
        // 移除k8s用户角色绑定
        Map<String, String> labels = new HashMap<>();
        labels.put(APP, ZEUS);
        roleBindingService.delete(cluster.getId(), null, null, labels);
        // 移除项目下分区绑定关系
        projectService.unBindNamespace(null ,null, cluster.getId(), null);
        // 删除集群和备份服务器的关联关系
        backupServerService.unbinding(cluster.getId());
        // 删除组织、项目下的资源分配
        organizationService.clear(cluster.getId());
        // 删除kube-config信息
        this.deleteKubeConfig(cluster.getId());
    }

    private void checkClusterExistent(MiddlewareClusterDTO cluster, boolean expectExisting) {
        // 获取已有集群信息
        List<MiddlewareClusterDTO> clusterList = new ArrayList<>();
        try {
            clusterList.addAll(listClusters(false, null, null, null));
        } catch (Exception e) {
        }
        // 校验内存中集群信息
        if (expectExisting) {
            // 期望集群存在 && 实际不存在
            if (clusterList.stream().noneMatch(clusterDTO -> clusterDTO.getId().equals(cluster.getId()))) {
                throw new BusinessException(DictEnum.CLUSTER, cluster.getName(), ErrorMessage.NOT_EXIST);
            }
            // 如果nickname重名
            if (clusterList.stream()
                .anyMatch(c -> !c.getId().equals(cluster.getId()) && c.getNickname().equals(cluster.getNickname()))) {
                throw new BusinessException(DictEnum.CLUSTER, cluster.getNickname(), ErrorMessage.EXIST);
            }
        } else {
            // 获取所有集群
            for (MiddlewareClusterDTO c : clusterList) {
                // 集群名称
                if (c.getId().equals(cluster.getId())) {
                    throw new BusinessException(DictEnum.CLUSTER, cluster.getName(), ErrorMessage.EXIST);
                }
                // 集群昵称
                if (c.getNickname().equals(cluster.getNickname())) {
                    throw new BusinessException(DictEnum.CLUSTER, cluster.getNickname(), ErrorMessage.EXIST);
                }
                // APIServer地址
                if (c.getHost().equals(cluster.getHost())) {
                    throw new BusinessException(DictEnum.CLUSTER, cluster.getAddress(), ErrorMessage.EXIST);
                }
            }
        }
    }


    private MiddlewareCluster convert(MiddlewareClusterDTO cluster) {
        ObjectMeta meta = new ObjectMeta();
        meta.setName(cluster.getName());
        meta.setNamespace(cluster.getDcId());
        if (cluster.getAttributes() != null && cluster.getAttributes().containsKey(CREATE_TIME)
                && cluster.getAttributes().get(CREATE_TIME) != null) {
            meta.setCreationTimestamp(cluster.getAttributes().get(CREATE_TIME).toString());
        } else {
            meta.setCreationTimestamp(DateUtils.dateToString(new Date(), DateStyle.YYYY_MM_DD_T_HH_MM_SS_Z));
        }
        Map<String, String> annotations = new HashMap<>();
        annotations.put(NAME, cluster.getNickname());
        meta.setAnnotations(annotations);
        MiddlewareClusterInfo clusterInfo = new MiddlewareClusterInfo();
        BeanUtils.copyProperties(cluster, clusterInfo);
        clusterInfo.setAddress(cluster.getHost());
        return new MiddlewareCluster().setMetadata(meta).setSpec(new MiddlewareClusterSpec().setInfo(clusterInfo));
    }

    public MiddlewareClusterDTO convertMiddlewareClusterToDto(MiddlewareCluster middlewareCluster) {
        MiddlewareClusterInfo info = middlewareCluster.getSpec().getInfo();
        MiddlewareClusterDTO cluster = new MiddlewareClusterDTO();
        BeanUtils.copyProperties(info, cluster);
        cluster.setId(K8sClient.getClusterId(middlewareCluster.getMetadata())).setHost(info.getAddress())
            .setName(middlewareCluster.getMetadata().getName()).setDcId(middlewareCluster.getMetadata().getNamespace())
            .setAnnotations(middlewareCluster.getMetadata().getAnnotations());
        if (!CollectionUtils.isEmpty(middlewareCluster.getMetadata().getAnnotations())) {
            cluster.setNickname(middlewareCluster.getMetadata().getAnnotations().get(NAME));
        }
        JSONObject attributes = new JSONObject();
        Date date = DateUtils.parseUTCDate(middlewareCluster.getMetadata().getCreationTimestamp());
        attributes.put(CREATE_TIME, date == null ? middlewareCluster.getMetadata().getCreationTimestamp()
            : DateUtils.DateToString(date, "yyyy-MM-dd HH:mm:ss"));
        cluster.setAttributes(attributes);
        return SerializationUtils.clone(cluster);
    }

    public void initMiddlewareChart() {
        List<BeanMiddlewareInfo> list = middlewareInfoService.list(true);
        if (!CollectionUtils.isEmpty(list)) {
            return;
        }
        File file = new File(middlewarePath);
        for (String name : file.list()) {
            ThreadPoolExecutorFactory.executor.execute(() -> {
                File f = new File(middlewarePath + File.separator + name);
                if (f.getAbsolutePath().contains(".tgz")) {
                    HelmChartFile chartFile = helmChartService.getHelmChartFromFile(null, null, f);
                    middlewareInfoService.insert(chartFile, f);
                    //middlewareAlertsService.updateAlerts2Mysql(chartFile);
                }
            });
        }
        middlewareCrTypeService.init();
    }

    public void insertMysqlImageRepository(MiddlewareClusterDTO clusterDTO) {
        ImageRepositoryDTO imageRepositoryDTO = imageRepositoryService.convertRegistry(clusterDTO.getRegistry());
        imageRepositoryService.insert(clusterDTO.getId(), imageRepositoryDTO);
    }

    @Override
    public String getClusterJoinCommand(String clusterName, String apiAddress, String userToken, Registry registry) {
        String clusterJoinUrl = apiAddress + "/clusters/quickAdd";
        String param = "name=" + clusterName;
        if (registry != null) {
            param = param + "&protocol=%s&address=%s&user=%s&&password=%s";
            param = String.format(param, registry.getProtocol(), registry.getAddress(),
                registry.getUser(), registry.getPassword());
            if (registry.getPort() != null){
                param = param + "&port=" + registry.getPort();
            }
        }
        String curlCommand =
            "curl -X POST --header 'Content-Type: multipart/form-data' --header 'userToken: %s' --header 'authType: 1' --form adminConf=@/etc/kubernetes/admin.conf \"%s?"
                + param + "\"";
        return String.format(curlCommand, userToken, clusterJoinUrl);
    }

    @Override
    public BaseResult quickAdd(MultipartFile adminConf, String name, Registry registry) {
        String filePath = uploadPath + "/" + adminConf.getName();
        try {
            File dir = new File(uploadPath);
            if (!dir.exists()) {
                if (!dir.mkdir()) {
                    log.error("文件夹创建失败");
                    return BaseResult.error();
                }
            }
            File file = new File(filePath);
            adminConf.transferTo(file);
        } catch (IOException e) {
            log.error("文件读取失败", e);
        }
        MiddlewareClusterDTO cluster = new MiddlewareClusterDTO();
        try {
            // 获取admin.conf全部内容
            String certificate = YamlUtil.convertToString(filePath);
            String serverAddress = YamlUtil.getServerAddress(filePath);
            // 删除admin.conf文件
            File file = new File(filePath);
            file.deleteOnExit();

            ClusterCert clusterCert = new ClusterCert();
            clusterCert.setCertificate(certificate);
            cluster.setCert(clusterCert);
            cluster.setName(name);
            cluster.setNickname(name);
            setClusterAddressInfo(cluster, serverAddress);
            // 设置镜像仓库
            if (registry != null) {
                registry.setType("harbor");
                registry.setChartRepo("middleware");
                cluster.setRegistry(registry);
            }
        } catch (Exception e) {
            log.error("集群添加失败", e);
            throw new BusinessException(DictEnum.CLUSTER, name, ErrorMessage.ADD_FAIL);
        }
        addCluster(cluster);
        return BaseResult.ok("集群添加成功");
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

    @Override
    public boolean checkIfExists(String clusterId) {
        QueryWrapper<BeanMiddlewareCluster> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id", clusterId);
        List<BeanMiddlewareCluster> clusters = middlewareClusterMapper.selectList(wrapper);
        return !CollectionUtils.isEmpty(clusters);
    }

    @Override
    @TranslateAfterResult
    public List<MonitorDto> getClusterMonitors(String clusterId) {
        //获取组件信息
        ClusterComponentsDto grafana = clusterComponentService.get(clusterId, "grafana");
        if (grafana == null) {
            throw new BusinessException(ErrorMessage.CLUSTER_MONITOR_INFO_NOT_FOUND);
        }
        MiddlewareClusterMonitorInfo monitorInfo = new MiddlewareClusterMonitorInfo();
        BeanUtils.copyProperties(grafana,monitorInfo);
        if (monitorInfo == null
                || StringUtils.isAnyEmpty(monitorInfo.getProtocol(), monitorInfo.getHost(), monitorInfo.getPort())) {
            throw new BusinessException(ErrorMessage.CLUSTER_MONITOR_INFO_NOT_FOUND);
        }
        // 生成token
        if (StringUtils.isEmpty(monitorInfo.getToken()) && StringUtils.isNotEmpty(monitorInfo.getUsername())
                && StringUtils.isNotEmpty(monitorInfo.getPassword())) {
            grafanaService.setToken(monitorInfo);
        }

        // 获取面板configmap
        HashMap<String, String> labels = new HashMap<>();
        labels.put("zeus_dashboard", "kubernetes");
        List<ConfigMap> monitorList = configMapService.list(clusterId, "monitoring", labels);
        List<MonitorDto> monitorDtoList = new ArrayList<>();
        if (CollectionUtils.isEmpty(monitorList)) {
            return null;
        }
        monitorList.forEach(cm -> {
            if (CollectionUtils.isEmpty(cm.getData())) {
                return;
            }
            String uid = null;
            String title = null;
            for (String value: cm.getData().values()) {
                JSONObject dashboardJSONConfig = JSONObject.parseObject(value);
                if (dashboardJSONConfig.containsKey("uid") && dashboardJSONConfig.containsKey("title")) {
                    uid = dashboardJSONConfig.getString("uid");
                    title = dashboardJSONConfig.getString("title");
                }
            }
            if (StringUtils.isBlank(uid) || StringUtils.isBlank(title)) {
                return;
            }
            String url = monitorInfo.getAddress() + "/d/" + uid;
            MonitorDto monitorDto = new MonitorDto().setAuthorization("Bearer " + monitorInfo.getToken()).setUrl(url).setTitle(title);
            monitorDtoList.add(monitorDto);
        });
        return monitorDtoList;
    }

    /**
     * 删除数据库中的kube_config信息
     * @param clusterId
     */
    private void deleteKubeConfig(String clusterId) {
        QueryWrapper<BeanKubeConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("cluster_id", clusterId);
        kubeConfigMapper.delete(wrapper);
    }

    /**
     * 设置集群地址信息
     * 
     * @param cluster
     *            集群
     * @param serverAddress
     *            集群master server信息
     */
    private void setClusterAddressInfo(MiddlewareClusterDTO cluster, String serverAddress) {
        String[] serverInfos = serverAddress.split(":");
        String host = serverInfos[1].replaceAll("//", "");
        cluster.setProtocol(serverInfos[0]);
        cluster.setHost(host);
        cluster.setPort(Integer.parseInt(serverInfos[2]));
    }

}
