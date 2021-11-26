package com.harmonycloud.zeus.service.middleware.impl;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.constants.CommonConstant;
import com.harmonycloud.caas.common.constants.NameConstant;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.MiddlewareServiceNameIndex;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.caas.common.model.user.ResourceMenuDto;
import com.harmonycloud.zeus.bean.BeanClusterMiddlewareInfo;
import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareInfo;
import com.harmonycloud.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.harmonycloud.zeus.schedule.MiddlewareManageTask;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.service.log.EsComponentService;
import com.harmonycloud.zeus.service.middleware.ClusterMiddlewareInfoService;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.tool.excel.ExcelUtil;
import com.harmonycloud.tool.page.PageObject;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCRD;
import com.harmonycloud.zeus.util.ServiceNameConvertUtil;
import com.harmonycloud.zeus.util.YamlUtil;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.zeus.operator.BaseOperator;
import com.harmonycloud.zeus.service.AbstractBaseService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;

import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_EXPOSE_NODEPORT;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.PODS;

/**
 * @author dengyulong
 * @date 2021/03/23
 * 处理中间件
 */
@Slf4j
@Service
public class MiddlewareServiceImpl extends AbstractBaseService implements MiddlewareService {

    @Autowired
    private MiddlewareCRDService middlewareCRDService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private MiddlewareManageTask middlewareManageTask;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private EsComponentService esComponentService;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private MiddlewareInfoService middlewareInfoService;
    @Autowired
    private IngressService ingressService;
    @Autowired
    private ClusterMiddlewareInfoService clusterMiddlewareInfoService;

    private final static Map<String, String> titleMap = new HashMap<String, String>(7) {
        {
            put("0", "慢日志采集时间");
            put("1", "sql语句");
            put("2", "客户端IP");
            put("3", "执行时长(s)");
            put("4", "锁定时长(s)");
            put("5", "解析行数");
            put("6", "返回行数");
        }
    };

    @Override
    public List<Middleware> simpleList(String clusterId, String namespace, String type, String keyword) {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);

        Map<String, String> label = null;
        List<String> nameList = new ArrayList<>();
        boolean nameFilter = false;
        if (StringUtils.isNotBlank(type) && MiddlewareTypeEnum.isType(type)) {
            label = new HashMap<>(1);
            label.put("type", MiddlewareTypeEnum.findByType(type).getMiddlewareCrdType());
        }
        else {
            nameList = getNameList(clusterId, namespace, type);
            nameFilter = true;
        }
        List<MiddlewareCRD> mwList = middlewareCRDService.listCR(clusterId, namespace, label);
        if (CollectionUtils.isEmpty(mwList)) {
            return new ArrayList<>(0);
        }
        //对于自定义中间件 根据名称进行过滤
        if (nameFilter) {
            if (CollectionUtils.isEmpty(nameList)) {
                return new ArrayList<>();
            }
            List<String> finalNameList = nameList;
            mwList = mwList.stream()
                .filter(mw -> finalNameList.stream().anyMatch(name -> mw.getSpec().getName().equals(name)))
                .collect(Collectors.toList());
        }

        // filter and convert
        Middleware middleware = new Middleware().setClusterId(clusterId).setNamespace(namespace).setType(type);
        Map<String, BaseOperator> operatorMap = new HashMap<>();
        boolean filter = StringUtils.isNotBlank(keyword);
        return mwList.stream().filter(mw -> !filter || mw.getMetadata().getName().contains(keyword)).map(mw -> {
            String middlewareType = MiddlewareTypeEnum.findTypeByCrdType(mw.getSpec().getType());
            if (!operatorMap.containsKey(middlewareType)) {
                middleware.setType(middlewareType);
                operatorMap.put(middlewareType, getOperator(BaseOperator.class, BaseOperator.class, middleware));
            }
            return operatorMap.get(middlewareType).convertByHelmChart(middlewareCRDService.simpleConvert(mw), cluster);
        }).collect(Collectors.toList());
    }

    @Override
    public Middleware detail(String clusterId, String namespace, String name, String type) {
        checkBaseParam(clusterId, namespace, name, type);
        Middleware middleware =
                new Middleware().setClusterId(clusterId).setNamespace(namespace).setType(type).setName(name);
        return getOperator(BaseOperator.class, BaseOperator.class, middleware).detail(middleware);
    }

    @Override
    public void create(Middleware middleware) {
        checkBaseParam(middleware);
        BaseOperator operator = getOperator(BaseOperator.class, BaseOperator.class, middleware);
        MiddlewareClusterDTO cluster = clusterService.findByIdAndCheckRegistry(middleware.getClusterId());
        // pre check
        operator.createPreCheck(middleware, cluster);
        // create
        middlewareManageTask.asyncCreate(middleware, cluster, operator);
    }

    @Override
    public void update(Middleware middleware) {
        checkBaseParam(middleware);
        BaseOperator operator = getOperator(BaseOperator.class, BaseOperator.class, middleware);
        MiddlewareClusterDTO cluster = clusterService.findByIdAndCheckRegistry(middleware.getClusterId());
        // pre check
        operator.updatePreCheck(middleware, cluster);
        // update
        middlewareManageTask.asyncUpdate(middleware, cluster, operator);
    }

    @Override
    public void delete(String clusterId, String namespace, String name, String type) {
        checkBaseParam(clusterId, namespace, name, type);
        Middleware middleware = new Middleware(clusterId, namespace, name, type);
        middlewareManageTask.asyncDelete(middleware, getOperator(BaseOperator.class, BaseOperator.class, middleware));
    }

    @Override
    public void switchMiddleware(String clusterId, String namespace, String name, String type, Boolean isAuto) {
        Middleware middleware = new Middleware(clusterId, namespace, name, type).setAutoSwitch(isAuto);
        middlewareManageTask.asyncSwitch(middleware, getOperator(BaseOperator.class, BaseOperator.class, middleware));
    }

    @Override
    public MonitorDto monitor(String clusterId, String namespace, String name, String type, String chartVersion) {
        Middleware middleware = new Middleware(clusterId, namespace, name, type);
        middleware.setChartVersion(chartVersion);
        return getOperator(BaseOperator.class, BaseOperator.class, middleware).monitor(middleware);
    }

    private void checkBaseParam(Middleware mw) {
        checkBaseParam(mw.getClusterId(), mw.getNamespace(), mw.getName(), mw.getType());
        if (StringUtils.isAnyBlank(mw.getChartName(), mw.getChartVersion())) {
            throw new IllegalArgumentException("chartName or chartVersion is null");
        }
    }

    private void checkBaseParam(String clusterId, String namespace, String name, String type) {
        if (StringUtils.isAnyBlank(clusterId, namespace, name, type)) {
            throw new IllegalArgumentException("middleware clusterId/namespace/name/type is null");
        }
    }

    @Override
    public List<Middleware> simpleListAll(String type) {

        List<MiddlewareClusterDTO> clusterList = clusterService.listClusters();
        List<Middleware> list = new ArrayList<>();
        clusterList.forEach(cluster -> {
            List<Namespace> namespaceList = namespaceService.list(cluster.getId(), true, null);
            namespaceList = namespaceList.stream().filter(Namespace::isRegistered).collect(Collectors.toList());
            namespaceList.forEach(namespace -> {
                List<Middleware> mwList = middlewareCRDService.list(cluster.getId(), namespace.getName(), type);
                list.addAll(mwList);
            });
        });
        return list;

    }

    @Override
    public PageObject<MysqlSlowSqlDTO> slowsql(SlowLogQuery slowLogQuery) throws Exception {
        MiddlewareClusterDTO cluster = clusterService.findById(slowLogQuery.getClusterId());
        PageObject<MysqlSlowSqlDTO> slowSqlDTOS = esComponentService.getSlowSql(cluster, slowLogQuery);
        return slowSqlDTOS;
    }

    @Override
    public void slowsqlExcel(SlowLogQuery slowLogQuery, HttpServletResponse response, HttpServletRequest request) throws Exception {
        slowLogQuery.setCurrent(1);
        slowLogQuery.setSize(CommonConstant.NUM_ONE_THOUSAND);
        PageObject<MysqlSlowSqlDTO> slowsql = slowsql(slowLogQuery);
        List<Map<String, Object>> demoValues = new ArrayList<>();
        slowsql.getData().stream().forEach(mysqlSlowSqlDTO -> {
            Map<String, Object> demoValue = new HashMap<String, Object>() {
                {
                    Date queryDate = DateUtils.parseUTCSDate(mysqlSlowSqlDTO.getTimestampMysql());
                    put("0", queryDate);
                    put("1", mysqlSlowSqlDTO.getQuery());
                    put("2", mysqlSlowSqlDTO.getClientip());
                    put("3", mysqlSlowSqlDTO.getQueryTime());
                    put("4", mysqlSlowSqlDTO.getLockTime());
                    put("5", mysqlSlowSqlDTO.getRowsExamined());
                    put("6", mysqlSlowSqlDTO.getRowsSent());
                }
            };
            demoValues.add(demoValue);
        });
        ExcelUtil.writeExcel(ExcelUtil.OFFICE_EXCEL_XLSX, "mysqlslowsql", null, titleMap, demoValues, response, request);
    }
    
    public List<String> getNameList(String clusterId, String namespace, String type) {
        // 获取中间件chartName + chartVersion
        List<BeanMiddlewareInfo> mwInfoList = middlewareInfoService.list(true);
        mwInfoList =
            mwInfoList.stream().filter(mwInfo -> mwInfo.getChartName().equals(type)).collect(Collectors.toList());
        List<String> chartList = mwInfoList.stream()
            .map(mwInfo -> mwInfo.getChartName() + "-" + mwInfo.getChartVersion()).collect(Collectors.toList());
        // 获取helm list信息
        List<HelmListInfo> helmInfoList = helmChartService.listHelm(namespace, "", clusterService.findById(clusterId));
        helmInfoList = helmInfoList.stream()
            .filter(helmInfo -> chartList.stream().allMatch(chart -> chart.equals(helmInfo.getChart())))
            .collect(Collectors.toList());
        return helmInfoList.stream().map(HelmListInfo::getName).collect(Collectors.toList());
    }

    @Override
    public <T, R> T getOperator(Class<T> funClass, Class<R> baseClass, Object... types) {
        return super.getOperator(funClass, baseClass, types);
    }

    @Override
    public void setManagePlatformAddress(Middleware middleware, String clusterId) {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        if (cluster.getIngress() == null) {
            return;
        }
        List<IngressDTO> ingressDTOS = ingressService.get(clusterId, middleware.getNamespace(), middleware.getType(), middleware.getName());
        MiddlewareServiceNameIndex serviceNameIndex = ServiceNameConvertUtil.convert(middleware);
        List<IngressDTO> serviceDTOList = ingressDTOS.stream().filter(ingressDTO -> (
                ingressDTO.getName().equals(serviceNameIndex.getNodePortServiceName()) && ingressDTO.getExposeType().equals(MIDDLEWARE_EXPOSE_NODEPORT))
        ).collect(Collectors.toList());

        if (!CollectionUtils.isEmpty(serviceDTOList)) {
            IngressDTO ingressDTO = serviceDTOList.get(0);
            String exposeIP = ingressDTO.getExposeIP();
            List<ServiceDTO> serviceList = ingressDTO.getServiceList();
            if (!CollectionUtils.isEmpty(serviceList)) {
                ServiceDTO serviceDTO = serviceList.get(0);
                String exposePort = serviceDTO.getExposePort();
                middleware.setManagePlatformAddress(exposeIP + ":" + exposePort);
            }
        } else {
            middleware.setManagePlatform(false);
        }
    }

    @Override
    public List<MiddlewareBriefInfoDTO> getMiddlewareBriefInfoList(List<MiddlewareClusterDTO> clusterDTOList) {
        List<BeanMiddlewareInfo> middlewareInfoList = middlewareInfoService.list(clusterDTOList);
        List<MiddlewareBriefInfoDTO> middlewareBriefInfoDTOList = new ArrayList<>();
        List<Middleware> middlewares = queryAllClusterService(clusterDTOList);
        middlewareInfoList.forEach(middlewareInfo -> {
            AtomicInteger serviceNum = new AtomicInteger();
            AtomicInteger errServiceNum = new AtomicInteger();
            MiddlewareBriefInfoDTO middlewareBriefInfoDTO = new MiddlewareBriefInfoDTO();
            countServiceNum(middlewares, middlewareInfo.getChartName(), serviceNum, errServiceNum);
            middlewareBriefInfoDTO.setName(middlewareInfo.getName());
            middlewareBriefInfoDTO.setChartName(middlewareInfo.getChartName());
            middlewareBriefInfoDTO.setVersion(middlewareInfo.getVersion());
            middlewareBriefInfoDTO.setChartVersion(middlewareInfo.getChartVersion());
            middlewareBriefInfoDTO.setImagePath(middlewareInfo.getImagePath());
            middlewareBriefInfoDTO.setServiceNum(serviceNum.get());
            middlewareBriefInfoDTO.setErrServiceNum(errServiceNum.get());
            middlewareBriefInfoDTOList.add(middlewareBriefInfoDTO);
        });
        try {
            Collections.sort(middlewareBriefInfoDTOList, new MiddlewareBriefInfoDTOComparator());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return middlewareBriefInfoDTOList;
    }

    @Override
    public List<ResourceMenuDto> listAllMiddlewareAsMenu(String clusterId) {
        List<ResourceMenuDto> subMenuList = new ArrayList<>();
        try {
            List<BeanClusterMiddlewareInfo> middlewareInfos = clusterMiddlewareInfoService.list(clusterId, true);
            if (CollectionUtils.isEmpty(middlewareInfos)) {
                return subMenuList;
            }
            AtomicInteger weight = new AtomicInteger(1);
            for (BeanClusterMiddlewareInfo middlewareInfoDTO : middlewareInfos) {
                if (middlewareInfoDTO.getStatus() == 2) {
                    //未安装的中间件不作为菜单展示
                    continue;
                }
                ResourceMenuDto resourceMenuDto = new ResourceMenuDto();
                resourceMenuDto.setName(middlewareInfoDTO.getChartName());
                resourceMenuDto.setAliasName(middlewareInfoDTO.getChartName());
                resourceMenuDto.setAvailable(true);
                resourceMenuDto.setWeight(weight.get());
                weight.getAndIncrement();
                subMenuList.add(resourceMenuDto);
            }
        } catch (Exception e) {
            log.error("查询服务列表错误", e);
        }
        return subMenuList;
    }

    @Override
    public List<MiddlewareBriefInfoDTO> listAllMiddleware(String clusterId, String namespace, String type, String keyword) {
        List<MiddlewareBriefInfoDTO> serviceList = null;
        try {
            ArrayList<MiddlewareClusterDTO> list = new ArrayList<>();
            list.add(clusterService.findById(clusterId));
            List<BeanMiddlewareInfo> middlewareInfoDTOList = middlewareInfoService.list(list);
            if (type != null) {
                middlewareInfoDTOList = middlewareInfoDTOList.stream().filter(middleware -> type.equals(middleware.getChartName())).collect(Collectors.toList());
                middlewareInfoDTOList.sort(Comparator.comparing(BeanMiddlewareInfo::getChartVersion).reversed());
                BeanMiddlewareInfo beanMiddlewareInfo = middlewareInfoDTOList.get(0);
                middlewareInfoDTOList.clear();
                middlewareInfoDTOList.add(beanMiddlewareInfo);
            }
            serviceList = new ArrayList<>();
            List<Middleware> middlewareServiceList = simpleList(clusterId, namespace, type, keyword);
            for (BeanMiddlewareInfo middlewareInfo : middlewareInfoDTOList) {
                AtomicInteger errServiceCount = new AtomicInteger(0);
                List<Middleware> singleServiceList = new ArrayList<>();
                for (Middleware middleware : middlewareServiceList) {
                    if (!middlewareInfo.getChartName().equals(middleware.getType())) {
                        continue;
                    }
                    MiddlewareCRD middlewareCRD = middlewareCRDService.getCR(clusterId, namespace, middlewareInfo.getType(), middleware.getName());
                    if (middlewareCRD != null && middlewareCRD.getStatus() != null && middlewareCRD.getStatus().getInclude() != null && middlewareCRD.getStatus().getInclude().get(PODS) != null) {
                        List<MiddlewareInfo> middlewareInfos = middlewareCRD.getStatus().getInclude().get(PODS);
                        middleware.setPodNum(middlewareInfos.size());
                        if (!NameConstant.RUNNING.equalsIgnoreCase(middleware.getStatus())) {
                            //中间件服务状态异常
                            errServiceCount.getAndAdd(1);
                        }
                        if (middleware.getManagePlatform() != null && middleware.getManagePlatform()) {
                            setManagePlatformAddress(middleware, clusterId);
                        }
                    }
                    singleServiceList.add(middleware);
                }
                MiddlewareBriefInfoDTO briefInfoDTO = new MiddlewareBriefInfoDTO();
                briefInfoDTO.setName(middlewareInfo.getName());
                briefInfoDTO.setImagePath(middlewareInfo.getImagePath());
                briefInfoDTO.setChartName(middlewareInfo.getChartName());
                briefInfoDTO.setChartVersion(middlewareInfo.getChartVersion());
                briefInfoDTO.setVersion(middlewareInfo.getVersion());
                Collections.sort(singleServiceList, new MiddlewareComparator());
                briefInfoDTO.setServiceList(singleServiceList);
                briefInfoDTO.setServiceNum(singleServiceList.size());
                briefInfoDTO.setOfficial(middlewareInfo.getOfficial());
                serviceList.add(briefInfoDTO);
            }
            Collections.sort(serviceList, new MiddlewareBriefInfoDTOComparator());
        } catch (Exception e) {
            log.error("查询服务列表错误", e);
        }
        return serviceList;
    }

    @Override
    public List<MiddlewareInfoDTO> version(String clusterId, String namespace, String name, String type) {
        List<BeanMiddlewareInfo> middlewareInfos = middlewareInfoService.listAllMiddlewareInfo(clusterId, type);
        // 将倒序转为正序
        middlewareInfos.sort(Comparator.comparing(BeanMiddlewareInfo::getChartVersion));
        Middleware middleware = detail(clusterId, namespace, name, type);
        AtomicBoolean existNow = new AtomicBoolean(false);
        List<MiddlewareInfoDTO> resList = middlewareInfos.stream().map(info -> {
            MiddlewareInfoDTO dto = new MiddlewareInfoDTO();
            BeanUtils.copyProperties(info, dto);
            if (info.getChartVersion().compareTo(middleware.getChartVersion()) < 0) {
                dto.setVersionStatus("history");
            } else if (info.getChartVersion().compareTo(middleware.getChartVersion()) == 0) {
                dto.setVersionStatus("now");
                existNow.set(true);
            } else {
                if (existNow.get()) {
                    BeanClusterMiddlewareInfo clusterMwInfo = clusterMiddlewareInfoService.get(clusterId, type);
                    if (!(clusterMwInfo.getChartVersion().compareTo(info.getChartVersion()) < 0)) {
                        if (clusterMwInfo.getStatus() == 0) {
                            // operator升级中
                            dto.setVersionStatus("updating");
                        } else {
                            // operator版本大于当前chart版本，可以升级
                            dto.setVersionStatus("canUpgrade");
                        }
                    } else {
                        // operator版本比当前chart版本小，需要升级operator版本
                        dto.setVersionStatus("needUpgradeOperator");
                    }
                    existNow.set(false);
                } else {
                    dto.setVersionStatus("future");
                }
            }
            return dto;
        }).collect(Collectors.toList());
        resList.sort(Comparator.comparing(MiddlewareInfoDTO::getChartVersion).reversed());
        return resList;
    }

    @Override
    public void upgradeChart(String clusterId, String namespace, String name, String type, String chartName, String upgradeChartVersion) {
        HelmChartFile helmChart = helmChartService.getHelmChartFromMysql(chartName, upgradeChartVersion);
        JSONObject upgradeValues = YamlUtil.convertYamlAsNormalJsonObject(helmChart.getValueYaml());
        JSONObject currentValues = helmChartService.getInstalledValuesAsNormalJson(name, namespace, clusterService.findById(clusterId));

        String currentChartVersion = currentValues.getString("chart-version");
        String compatibleVersions = upgradeValues.getString("compatibleVersions");
        // 检查chart版本是否符合升级要求
        checkUpgradeVersion(currentChartVersion, upgradeChartVersion, compatibleVersions);
        // 检查operator是否符合升级要求
        checkOperatorVersion(upgradeChartVersion, clusterMiddlewareInfoService.get(clusterId, type));

        JSONObject upgradeImage = SerializationUtils.clone(upgradeValues.getJSONObject("image"));
        JSONObject currentImage = SerializationUtils.clone(currentValues.getJSONObject("image"));
        if (currentImage.getString("repository") != null) {
            upgradeImage.put("repository", currentImage.getString("repository"));
        }
        JSONObject resImage = YamlUtil.jsonMerge(upgradeImage, currentImage);

        JSONObject resValues = YamlUtil.jsonMerge(currentValues, upgradeValues);
        resValues.put("image", resImage);
        resValues.put("chart-version", upgradeChartVersion);
        // 执行升级
        Middleware middleware = detail(clusterId, namespace, name, type);
        middleware.setChartName(chartName);
        middlewareManageTask.asyncUpdateChart(middleware, currentValues, upgradeValues, upgradeChartVersion, clusterService.findById(clusterId), helmChartService);
    }

    /**
     * 转换中间件简要信息
     * @param middlewareInfoDTO
     * @param singleServiceList
     * @return
     */
    private MiddlewareBriefInfoDTO convertMiddlewareBriefInfo(MiddlewareInfoDTO middlewareInfoDTO, List<Middleware> singleServiceList) {
        MiddlewareBriefInfoDTO briefInfoDTO = new MiddlewareBriefInfoDTO();
        briefInfoDTO.setName(middlewareInfoDTO.getName());
        briefInfoDTO.setImagePath(middlewareInfoDTO.getImagePath());
        briefInfoDTO.setChartName(middlewareInfoDTO.getChartName());
        briefInfoDTO.setChartVersion(middlewareInfoDTO.getChartVersion());
        briefInfoDTO.setVersion(middlewareInfoDTO.getVersion());
        briefInfoDTO.setOfficial(middlewareInfoDTO.getOfficial());
        briefInfoDTO.setServiceList(singleServiceList);
        briefInfoDTO.setServiceNum(singleServiceList.size());
        return briefInfoDTO;
    }

    /**
     * @description 统计指定类型服务数量和异常服务数量
     * @param middlewares 所有服务
     * @param chartName 中间件chartName
     * @param serviceNum 服务数量
     * @param errServiceNum 异常服务数量
     */
    private void countServiceNum(List<Middleware> middlewares, String chartName, AtomicInteger serviceNum, AtomicInteger errServiceNum) {
        for (Middleware middleware : middlewares) {
            try {
                if (!chartName.equals(middleware.getType())) {
                    continue;
                }
                serviceNum.getAndAdd(1);
                if (middleware.getStatus() != null) {
                    if (!NameConstant.RUNNING.equalsIgnoreCase(middleware.getStatus())) {
                        //中间件服务状态异常
                        errServiceNum.getAndAdd(1);
                    }
                } else {
                    errServiceNum.getAndAdd(1);
                }
            } catch (Exception e) {
                log.error("查询中间件CR出错了,chartName={},type={}", chartName, e);
            }
        }
    }

    /**
     * 查询集群所有服务
     * @param clusterDTOList 集群列表
     * @return
     */
    private List<Middleware> queryAllClusterService(List<MiddlewareClusterDTO> clusterDTOList) {
        List<Namespace> namespaceList = new ArrayList<>();
        clusterDTOList.forEach(cluster -> {
            List<Namespace> namespaces = namespaceService.list(cluster.getId(), true, null);
            namespaces = namespaces.stream().filter(Namespace::isRegistered).collect(Collectors.toList());
            namespaceList.addAll(namespaces);
        });
        List<Middleware> middlewareServiceList = new ArrayList<>();
        namespaceList.forEach(namespace -> {
            List<Middleware> middlewares = simpleList(namespace.getClusterId(), namespace.getName(), null, "");
            middlewareServiceList.addAll(middlewares);
        });
        return middlewareServiceList;
    }

    /**
     * 服务排序类，按服务数量进行排序
     */
    public static class ServiceMapComparator implements Comparator<Map> {
        @Override
        public int compare(Map service1, Map service2) {
            if (Integer.parseInt(service1.get("serviceNum").toString()) > Integer.parseInt(service2.get("serviceNum").toString())) {
                return -1;
            } else if (Integer.parseInt(service1.get("serviceNum").toString()) == Integer.parseInt(service2.get("serviceNum").toString())) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    /**
     * 服务排序类，按服务数量进行排序
     */
    public static class MiddlewareBriefInfoDTOComparator implements Comparator<MiddlewareBriefInfoDTO> {
        @Override
        public int compare(MiddlewareBriefInfoDTO o1, MiddlewareBriefInfoDTO o2) {
            if (o1.getServiceNum() > o2.getServiceNum()) {
                return -1;
            } else if (o1.getServiceNum() == o2.getServiceNum()) {
                return 0;
            } else {
                return 1;
            }
        }
    }

    /**
     * 中间件类型排序类
     */
    public static class MiddlewareInfoDTOComparator implements Comparator<MiddlewareInfoDTO> {
        @Override
        public int compare(MiddlewareInfoDTO o1, MiddlewareInfoDTO o2) {
            int temp = o1.getChartName().compareTo(o2.getChartName());
            if (temp > 0) {
                return 1;
            } else if (temp < 0) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    /**
     * 服务排序类，按创建时间排序
     */
    public static class MiddlewareComparator implements Comparator<Middleware> {
        @Override
        public int compare(Middleware o1, Middleware o2) {
            int res = o1.getCreateTime().compareTo(o2.getCreateTime());
            if (res > 0) {
                return -1;
            } else if (res < 0) {
                return 1;
            } else {
                return 0;
            }
        }
    }

    /**
     * 检查中间件chart升级版本是否符合要求
     * @param currentVersion 中间件当前版本
     * @param upgradeVersion 中间件升级版本
     * @param compatibleVersions 升级版本所需最低版本
     * @return
     */
    private static void checkUpgradeVersion(String currentVersion, String upgradeVersion, String compatibleVersions) {
        // 1.判断是否升级到了低版本
        if (currentVersion.compareTo(upgradeVersion) > 0) {
            log.error("不能升级到更低版本");
            throw new BusinessException(ErrorMessage.UPGRADE_LOWER_VERSION_FAILED);
        }
        // 2.判断是否跨大版本升级,即判断大版本号是否相同，相同才可以升级
        String current = currentVersion.split("\\.")[0];
        String upgrade = upgradeVersion.split("\\.")[0];
        if (!current.equals(upgrade)) {
            throw new BusinessException(ErrorMessage.UPGRADE_OVER_VERSION_FAILED);
        }
        // 3.判断是否满足升级至当前chart所需最低版本
        if (StringUtils.isBlank(compatibleVersions)) {
            return;
        }
        if (currentVersion.compareTo(compatibleVersions) < 0) {
            log.error("不满足升级所需最低版本");
            throw new BusinessException(ErrorMessage.UPGRADE_NOT_SATISFY_LOWEST_VERSION);
        }
    }

    /**
     * 检查当前operator版本是否满足升级要求，operator版本需要比升级chart版本高，才可以进行升级，否则需要升级operator
     * @param upgradeChartVersion
     * @param clusterMwInfo
     */
    private static void checkOperatorVersion(String upgradeChartVersion,BeanClusterMiddlewareInfo clusterMwInfo){
        if (!(clusterMwInfo.getChartVersion().compareTo(upgradeChartVersion) < 0)) {
            if (clusterMwInfo.getStatus() == 0) {
                log.warn("operator升级中");
                throw new BusinessException(ErrorMessage.UPGRADE_OPERATOR_UPDATING);
            }
        } else {
            log.warn("operator版本比升级chart版本小，需要升级operator版本");
            throw new BusinessException(ErrorMessage.UPGRADE_OPERATOR_TOO_LOWER);
        }
    }

}
