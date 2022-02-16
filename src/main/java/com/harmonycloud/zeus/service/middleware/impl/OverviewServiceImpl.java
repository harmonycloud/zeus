package com.harmonycloud.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.harmonycloud.caas.common.constants.NameConstant;
import com.harmonycloud.caas.common.enums.DateType;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.enums.middleware.ResourceUnitEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.exception.CaasRuntimeException;
import com.harmonycloud.caas.common.model.*;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.tool.numeric.ResourceCalculationUtil;
import com.harmonycloud.zeus.bean.*;
import com.harmonycloud.zeus.dao.BeanAlertRecordMapper;
import com.harmonycloud.zeus.dao.MiddlewareAlertInfoMapper;
import com.harmonycloud.zeus.integration.cluster.PrometheusWrapper;
import com.harmonycloud.zeus.integration.cluster.ResourceQuotaWrapper;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCRD;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareInfo;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareSpec;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareStatus;
import com.harmonycloud.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRService;
import com.harmonycloud.zeus.service.k8s.NamespaceService;
import com.harmonycloud.zeus.service.k8s.ResourceQuotaService;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.service.middleware.OverviewService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.zeus.service.system.OperationAuditService;
import com.harmonycloud.zeus.util.AlertDataUtil;
import com.harmonycloud.zeus.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.CommonConstant.LINE;
import static com.harmonycloud.caas.common.constants.NameConstant.CPU;
import static com.harmonycloud.caas.common.constants.NameConstant.MEMORY;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.PERSISTENT_VOLUME_CLAIMS;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.PODS;

/**
 * @author xutianhong
 * @Date 2021/3/26 2:35 下午
 */
@Slf4j
@Service
public class OverviewServiceImpl implements OverviewService {

    @Autowired
    private PrometheusWrapper prometheusWrapper;
    @Autowired
    private MiddlewareCRService middlewareCRService;
    @Autowired
    protected HelmChartService helmChartService;
    @Autowired
    private MiddlewareInfoService middlewareInfoService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private ResourceQuotaService resourceQuotaService;
    @Autowired
    private BeanAlertRecordMapper beanAlertRecordMapper;
    @Autowired
    private ResourceQuotaWrapper resourceQuotaWrapper;
    @Autowired
    private MiddlewareService middlewareService;
    @Autowired
    private OperationAuditService operationAuditService;
    @Autowired
    private MiddlewareAlertInfoMapper middlewareAlertInfoMapper;
    @Autowired
    private MiddlewareAlertsServiceImpl middlewareAlertsService;

    @Value("${system.platform.version:v0.1.0}")
    private String version;
    /**
     * 查询中间件状态
     *
     * @param clusterId
     *            集群id
     * @param namespace
     *            命名空间
     * @return
     */
    @Override
    public List<MiddlewareStatusDto> getMiddlewareStatus(String clusterId, String namespace) {

        // 封装middleware
        List<Middleware> middlewares = middlewareCRService.list(clusterId, namespace, null, true);
        if (CollectionUtils.isEmpty(middlewares)) {
            return null;
        }
        Map<String, List<Middleware>> middlewareMap =
            middlewares.stream().collect(Collectors.groupingBy(Middleware::getType));
        //获取imagePath
        List<BeanMiddlewareInfo> mwInfoList =  middlewareInfoService.list(true);

        List<MiddlewareStatusDto> middlewareStatusDtoList = new ArrayList<>();
        middlewareMap.forEach((key, value) -> {
            MiddlewareStatusDto middlewareStatusDto = new MiddlewareStatusDto();
            middlewareStatusDto.setType(key);
            middlewareStatusDto.setStatus(true);
            middlewareStatusDto.setMiddlewareList(value);
            double totalCpu = 0.0;
            double totalMemory = 0.0;
            for (Middleware middleware : value) {
                double cpu = ResourceCalculationUtil
                    .getResourceValue(middleware.getQuota().get(middleware.getType()).getCpu(), CPU, "");
                double memory = ResourceCalculationUtil.getResourceValue(
                    middleware.getQuota().get(middleware.getType()).getMemory(), NameConstant.MEMORY,
                    ResourceUnitEnum.G.getUnit());

                middleware.getQuota().get(middleware.getType())
                    .setCpu(BigDecimal.valueOf(cpu).setScale(2, RoundingMode.CEILING).toString());
                middleware.getQuota().get(middleware.getType())
                    .setMemory(BigDecimal.valueOf(memory).setScale(2, RoundingMode.CEILING).toString());

                totalCpu = totalCpu + cpu;
                totalMemory = totalMemory + memory;
                if (!NameConstant.RUNNING.equalsIgnoreCase(middleware.getStatus())) {
                    middlewareStatusDto.setStatus(false);
                }
            }
            middlewareStatusDto
                .setTotalCpu(BigDecimal.valueOf(totalCpu).setScale(2, RoundingMode.CEILING).doubleValue());
            middlewareStatusDto
                .setTotalMemory(BigDecimal.valueOf(totalMemory).setScale(2, RoundingMode.CEILING).doubleValue());
            middlewareStatusDto.setImagePath(mwInfoList.stream().filter(mw -> mw.getChartName().equals(key))
                .collect(Collectors.toList()).get(0).getImagePath());
            middlewareStatusDtoList.add(middlewareStatusDto);
        });
        return middlewareStatusDtoList;
    }

    @Override
    public List<MiddlewareMonitorDto> getMonitorInfo(String clusterId, String namespace, String name, String type,
        String startTime, String endTime) throws Exception {

        // 获取中间件crd对象
        MiddlewareCRD middlewareCRD = middlewareCRService.getCR(clusterId, namespace, type, name);

        // 获取pod列表
        if (ObjectUtils.isEmpty(middlewareCRD.getStatus().getInclude())) {
            throw new CaasRuntimeException(ErrorMessage.MIDDLEWARE_CLUSTER_STATUS_ABNORMAL);
        }
        List<MiddlewareInfo> podInfo = middlewareCRD.getStatus().getInclude().get(PODS);
        if (CollectionUtils.isEmpty(podInfo)) {
            return new ArrayList<>(0);
        }

        // 生成queryMap
        Map<String, String> queryMap = convertQueryMap(startTime, endTime);
        if (CollectionUtils.isEmpty(queryMap)) {
            return new ArrayList<>(0);
        }

        StringBuilder pods = new StringBuilder();
        for (MiddlewareInfo middlewareInfo : podInfo) {
            pods.append(middlewareInfo.getName()).append("|");
        }
        // 获取pvc列表
        List<MiddlewareInfo> pvcInfo = middlewareCRD.getStatus().getInclude().get(PERSISTENT_VOLUME_CLAIMS);
        StringBuilder pvcs = new StringBuilder();
        if (!CollectionUtils.isEmpty(pvcInfo)) {
            for (MiddlewareInfo middlewareInfo : pvcInfo) {
                pvcs.append(middlewareInfo.getName()).append("|");
            }
        }

        // 获取cpu使用情况
        String cpuUsedQuery = "sum by (container_name)(rate(container_cpu_usage_seconds_total{pod=~\"" + pods.toString()
            + "\",namespace=\"" + namespace + "\"}[1m]))";
        queryMap.put("query", cpuUsedQuery);
        PrometheusResponse prometheusCpuUsed =
            prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION_RANGE, queryMap);
        // 没有cpu使用量，不用继续查了，直接返回
        if (prometheusCpuUsed.getData().getResult().size() == 0) {
            return new ArrayList<>(0);
        }

        // 获取cpu申请配额
        String cpuRequestQuery = "sum by (container_name)(container_spec_cpu_quota{pod=~\"" + pods.toString()
            + "\",namespace=\"" + namespace + "\"}/100000)";
        queryMap.put("query", cpuRequestQuery);
        PrometheusResponse prometheusCpuRequest =
            prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION_RANGE, queryMap);
        // 获取memory使用情况
        String memoryUsedQuery = "sum by(container_name) (container_memory_working_set_bytes{namespace=\"" + namespace
            + "\", pod=~\"" + pods.toString() + "\"})";
        queryMap.put("query", memoryUsedQuery);
        PrometheusResponse prometheusMemoryUsed =
            prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION_RANGE, queryMap);
        // 获取memory申请配额
        String memoryRequestQuery = "sum by(container_name) (container_spec_memory_limit_bytes{namespace=\"" + namespace
            + "\", pod=~\"" + pods.toString() + "\"})";
        queryMap.put("query", memoryRequestQuery);
        PrometheusResponse prometheusMemoryRequest =
            prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION_RANGE, queryMap);

        // 获取卷使用量
        String storageUsedQuery = "sum(kubelet_volume_stats_used_bytes{namespace=\"" + namespace
            + "\",persistentvolumeclaim=~\"" + pvcs.toString() + "\"})";
        queryMap.put("query", storageUsedQuery);
        PrometheusResponse prometheusStorageUsed =
            prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION_RANGE, queryMap);

        // 获取卷申请量
        String storageRequestQuery = "sum(kubelet_volume_stats_capacity_bytes{namespace=\"" + namespace
            + "\",persistentvolumeclaim=~\"" + pvcs.toString() + "\"})";
        queryMap.put("query", storageRequestQuery);
        PrometheusResponse prometheusStorageRequest =
            prometheusWrapper.get(clusterId, NameConstant.PROMETHEUS_API_VERSION_RANGE, queryMap);

        List<PrometheusResult> cpuUsedResult = prometheusCpuUsed.getData().getResult();
        List<PrometheusResult> cpuRequestResult = prometheusCpuRequest.getData().getResult();
        List<PrometheusResult> memoryUsedResult = prometheusMemoryUsed.getData().getResult();
        List<PrometheusResult> memoryRequestResult = prometheusMemoryRequest.getData().getResult();
        List<PrometheusResult> storageUsedResult = prometheusStorageUsed.getData().getResult();
        List<PrometheusResult> storageRequestResult = prometheusStorageRequest.getData().getResult();

        cpuRequestResult = checkData(cpuUsedResult, cpuRequestResult);
        memoryRequestResult = checkData(memoryUsedResult, memoryRequestResult);
        storageRequestResult = checkData(storageUsedResult, storageRequestResult);

        // 判断是否有数据，避免NPE
        boolean hasCpu = !CollectionUtils.isEmpty(cpuUsedResult)
            && !CollectionUtils.isEmpty(cpuUsedResult.get(0).getValues()) && !CollectionUtils.isEmpty(cpuRequestResult)
            && !CollectionUtils.isEmpty(cpuRequestResult.get(0).getValues());
        boolean hasMemory =
            !CollectionUtils.isEmpty(memoryUsedResult) && !CollectionUtils.isEmpty(memoryUsedResult.get(0).getValues())
                && !CollectionUtils.isEmpty(memoryRequestResult)
                && !CollectionUtils.isEmpty(memoryRequestResult.get(0).getValues());
        boolean hasStorage = !CollectionUtils.isEmpty(storageUsedResult)
            && !CollectionUtils.isEmpty(storageUsedResult.get(0).getValues())
            && !CollectionUtils.isEmpty(storageRequestResult)
            && !CollectionUtils.isEmpty(storageRequestResult.get(0).getValues());
        if (!hasCpu && !hasMemory && !hasStorage) {
            return new ArrayList<>(0);
        }

        List<MiddlewareMonitorDto> middlewareMonitorDtoList = new ArrayList<>();

        for (int i = 0; i < cpuUsedResult.get(0).getValues().size(); ++i) {
            MiddlewareMonitorDto middlewareMonitorDto = new MiddlewareMonitorDto();
            middlewareMonitorDto.setTime(getDate(cpuUsedResult.get(0).getValues().get(i).get(0)));
            // cpu
            if (hasCpu) {
                double cpuRate = Double.parseDouble(cpuUsedResult.get(0).getValues().get(i).get(1))
                    / Double.parseDouble(cpuRequestResult.get(0).getValues().get(i).get(1)) * 100;
                double cpu = Double.parseDouble(cpuUsedResult.get(0).getValues().get(i).get(1)) * 1000;
                middlewareMonitorDto.setCpuRate(
                    ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(cpuRate), 2, RoundingMode.CEILING));
                middlewareMonitorDto
                    .setCpu(ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(cpu), 2, RoundingMode.CEILING));
                middlewareMonitorDto.setCpuTotal(ResourceCalculationUtil.roundNumber(
                    BigDecimal.valueOf(Double.parseDouble(cpuRequestResult.get(0).getValues().get(i).get(1)) * 1000), 2,
                    RoundingMode.FLOOR));
            }
            // memory
            if (hasMemory && i < memoryUsedResult.get(0).getValues().size()) {
                double memoryRate = Double.parseDouble(memoryUsedResult.get(0).getValues().get(i).get(1))
                    / Double.parseDouble(memoryRequestResult.get(0).getValues().get(i).get(1)) * 100;
                double memory = Double.parseDouble(memoryUsedResult.get(0).getValues().get(i).get(1)) / 1024 / 1024;
                middlewareMonitorDto.setMemoryRate(
                    ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(memoryRate), 2, RoundingMode.CEILING));
                middlewareMonitorDto.setMemory(
                    ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(memory), 2, RoundingMode.CEILING));
                middlewareMonitorDto.setMemoryTotal(ResourceCalculationUtil.roundNumber(
                    BigDecimal.valueOf(
                        Double.parseDouble(memoryRequestResult.get(0).getValues().get(i).get(1)) / 1024 / 1024),
                    2, RoundingMode.FLOOR));
            }
            // storage
            if (hasStorage && i < storageUsedResult.get(0).getValues().size()) {
                double storageRate = Double.parseDouble(storageUsedResult.get(0).getValues().get(i).get(1))
                    / Double.parseDouble(storageRequestResult.get(0).getValues().get(i).get(1)) * 100;
                double storage =
                    Double.parseDouble(storageUsedResult.get(0).getValues().get(i).get(1)) / 1024 / 1024 / 1024;
                middlewareMonitorDto.setStorage(
                    ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(storage), 2, RoundingMode.CEILING));
                middlewareMonitorDto.setStorageRate(
                    ResourceCalculationUtil.roundNumber(BigDecimal.valueOf(storageRate), 2, RoundingMode.CEILING));
                middlewareMonitorDto.setStorageTotal(ResourceCalculationUtil.roundNumber(
                    BigDecimal.valueOf(
                        Double.parseDouble(storageRequestResult.get(0).getValues().get(i).get(1)) / 1024 / 1024 / 1024),
                    2, RoundingMode.FLOOR));
            }
            middlewareMonitorDtoList.add(middlewareMonitorDto);
        }
        return middlewareMonitorDtoList;
    }

    @Override
    public PageInfo<AlertDTO> getAlertRecord(String clusterId, String namespace,
                                             String middlewareName, Integer current,
                                             Integer size, String level,
                                             String keyword, String lay) {
        if (current != null && size != null) {
            PageHelper.startPage(current, size);
        }
        QueryWrapper<BeanAlertRecord> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(lay)) {
            wrapper.eq("lay",lay);
        }
        if (StringUtils.isNotEmpty(clusterId) && StringUtils.isNotEmpty(namespace)) {
            if ("system".equals(lay)) { //系统告警记录
                    wrapper.eq("cluster_id", clusterId).eq("namespace", NameConstant.MONITORING).eq("name", NameConstant.PROMETHEUS_K8S_RULES);
            }
            if ("service".equals(lay)){ //服务告警记录
                    wrapper.eq("cluster_id", clusterId).eq("namespace", namespace).eq("name", middlewareName);
            }
        } else if (StringUtils.isNotEmpty(clusterId) && StringUtils.isEmpty(namespace)) {
            wrapper.eq("cluster_id", clusterId).eq("namespace", NameConstant.MONITORING).eq("name", NameConstant.PROMETHEUS_K8S_RULES);
        } else {
            wrapper.isNotNull("cluster_id").isNotNull("namespace").ne("cluster_id", "").eq("lay","service");
        }

        if (StringUtils.isNotEmpty(level)) {
            wrapper.eq("level", level);
        }

        if (StringUtils.isNotEmpty(keyword)) {
            String alertID = keyword.replaceAll("GJ","");
            if (middlewareAlertsService.isNumeric(alertID)) {
                wrapper.and(queryWrapper -> {
                    queryWrapper.like("id",Integer.parseInt(alertID)).or().like("alert_id",Integer.parseInt(alertID));
                });
            } else if (alertID.contains("-")) {
                String[] alert = alertID.split("-");
                if (alert.length == 1) {
                    wrapper.and(queryWrapper -> {
                        queryWrapper.like("id",Integer.parseInt(alert[0])).or().like("alert_id",Integer.parseInt(alert[0]));
                    });
                }
                if (alert.length == 2) {
                    wrapper.and(queryWrapper -> {
                        queryWrapper.like("id",Integer.parseInt(alert[1])).like("alert_id",Integer.parseInt(alert[0]));
                    });
                }
            } else {
                wrapper.and(queryWrapper -> {
                    queryWrapper.eq("id",keyword).or().like("alert",keyword)
                            .or().like("content",keyword).or().like("expr",keyword)
                            .or().like("summary",keyword).or().like("message",keyword);
                });
            }
        }

        //最近24小时
        if (StringUtils.isEmpty(clusterId)) {
            Date date = new Date();
            String end = DateFormatUtils.format(date, "yyyy-MM-dd HH:mm:ss");
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);
            calendar.add(Calendar.DAY_OF_MONTH, -1);
            String begin = DateFormatUtils.format(calendar.getTime(), "yyyy-MM-dd HH:mm:ss");

            wrapper.ge("time",begin);
            wrapper.le("time",end);
        }
        wrapper.orderByDesc("id");
        List<BeanAlertRecord> recordList = beanAlertRecordMapper.selectList(wrapper);
        PageInfo<AlertDTO> alertDTOPage = new PageInfo<>();
        BeanUtils.copyProperties(new PageInfo<>(recordList), alertDTOPage);
        if (CollectionUtils.isEmpty(recordList)) {
            return alertDTOPage;
        }
        Map<Middleware, String> middlewareMap = new HashMap<>();
        alertDTOPage.setList(recordList.stream().map(record -> {
            AlertDTO alertDTO = new AlertDTO();
            BeanUtils.copyProperties(record, alertDTO);
            Middleware middleware = new Middleware().setName(alertDTO.getName()).setNamespace(alertDTO.getNamespace())
                .setClusterId(alertDTO.getClusterId());
            if (middlewareMap.containsKey(middleware)) {
                alertDTO.setChartVersion(middlewareMap.get(middleware));
            } else {
                try {
                    JSONObject values = helmChartService.getInstalledValues(middleware, clusterService.findById(alertDTO.getClusterId()));
                    if (values != null && values.containsKey("chart-version")) {
                        String version = values.getString("chart-version");
                        middlewareMap.put(middleware, version);
                        alertDTO.setChartVersion(version);
                    } else {
                        middlewareMap.put(middleware, null);
                        alertDTO.setChartVersion(null);
                    }
                } catch (Exception e) {
                    alertDTO.setChartVersion(null);
                }
            }
           if (record.getAlertId() != null) {
                //告警记录ID
                alertDTO.setAlertId(middlewareAlertsService.calculateID(record.getAlertId())
                        + "-" + middlewareAlertsService.createId(record.getId()));
            }
          //返回大写类型
           changeType(record,alertDTO);
           return alertDTO;
        }).collect(Collectors.toList()));
        alertDTOPage.getList().sort(
            (o1, o2) -> o1.getTime() == null ? -1 : o2.getTime() == null ? -1 : o2.getTime().compareTo(o1.getTime()));
        return alertDTOPage;
    }

    /**
     * 转换时间格式，并计算step
     *
     * @param startTime
     * @param endTime
     * @return Map<String, String>
     */
    public Map<String, String> convertQueryMap(String startTime, String endTime) {
        Map<String, String> queryMap = new HashMap<>();
        // 计算step
        Date start = DateUtils.parseDate(startTime, DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue());
        Date end = DateUtils.parseDate(endTime, DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue());
        Date now = new Date();
        // 开始时间大于当前时间，直接返回空数据
        if (now.before(start)) {
            return null;
        }
        // 结束时间最大为当前时间
        if (end.after(now)) {
            end = now;
        }
        if (start.after(end)) {
            throw new BusinessException(ErrorMessage.TIME_PICK_ERROR);
        }
        long diff = end.getTime() - start.getTime();
        String step = "8h";
        if (diff < 7 * 24 * 60 * 60 * 1000) {
            step = String.valueOf(diff / 20 / 1000);
        }
        queryMap.put("step", step);
        queryMap.put("end", DateUtils.formatDate(end.getTime(), DateType.YYYY_MM_DD_T_HH_MM_SS_Z_SSS.getValue(),
            TimeZone.getTimeZone("GMT")));
        queryMap.put("start", DateUtils.formatDate(start.getTime(),
            DateType.YYYY_MM_DD_T_HH_MM_SS_Z_SSS.getValue(), TimeZone.getTimeZone("GMT")));
        return queryMap;
    }

    /**
     * 转换时间
     */
    public String getDate(String time) {
        return DateUtils.formatDate((Long.parseLong(time.substring(0, 10)) * 1000),
            DateType.YYYY_MM_DD_HH_MM_SS.getValue(), TimeZone.getTimeZone("GMT+8"));
    }

    /**
     * 填充丢失数据
     */
    public List<PrometheusResult> checkData(List<PrometheusResult> usedResult, List<PrometheusResult> requestResult) {
        if (!CollectionUtils.isEmpty(usedResult) && !CollectionUtils.isEmpty(requestResult)) {
            if (usedResult.get(0).getValues().size() != requestResult.get(0).getValues().size()) {
                // 转换为map
                Map<String, List<String>> usedResultMap = usedResult.get(0).getValues().stream()
                    .collect(Collectors.toMap(values -> values.get(0), values -> values));
                Map<String, List<String>> requestResultMap = requestResult.get(0).getValues().stream()
                    .collect(Collectors.toMap(values -> values.get(0), values -> values));
                // 根据key排序
                TreeMap<String, List<String>> usedResultTreeMap = new TreeMap<>(usedResultMap);
                TreeMap<String, List<String>> requestResultTreeMap = new TreeMap<>(requestResultMap);
                List<List<String>> result = new ArrayList<>();
                List<String> lastRequestDate = new ArrayList<>(requestResult.get(0).getValues().get(0));
                for (String time : usedResultTreeMap.keySet()) {
                    if (!requestResultTreeMap.containsKey(time)) {
                        lastRequestDate.set(0, time);
                        result.add(lastRequestDate);
                    } else {
                        result.add(requestResultTreeMap.get(time));
                        lastRequestDate.clear();
                        lastRequestDate.addAll(requestResultTreeMap.get(time));
                    }
                }
                requestResult.get(0).setValues(result);
            }
        }
        return requestResult;
    }

    /**
     * 平台总览
     *
     * @return middlewareOverviewDTO
     */
    @Override
    public MiddlewareOverviewDTO getChartPlatformOverview(){
        MiddlewareOverviewDTO overview = new MiddlewareOverviewDTO();

        List<MiddlewareClusterDTO> clusterDTOS = clusterService.listClusters();
        // 集群数
        overview.setTotalClusterCount(clusterDTOS.size());
        // 集群列表详细
        overview.setClusters(new ArrayList<>());
//        Map<String, OverviewNamespaceInfo> namespaceMap = new HashMap<>();
        clusterDTOS.forEach(clusterDTO -> {

            OverviewClusterInfo overviewClusterInfo = new OverviewClusterInfo();
            overviewClusterInfo.setClusterId(clusterDTO.getId());
            overviewClusterInfo.setClusterName(clusterDTO.getName());
            overview.getClusters().add(overviewClusterInfo);

            overviewClusterInfo.setNamespaces(new ArrayList<>());

            List<Namespace> namespaces = namespaceService.list(clusterDTO.getId(), false, false, false, null);
            if (CollectionUtils.isEmpty(namespaces)) {
                return;
            }
            Map<String, OverviewNamespaceInfo> namespaceMap = namespaces.stream().filter(namespace -> namespace.isRegistered()).map(namespace -> {
                OverviewNamespaceInfo overviewNSInfo = new OverviewNamespaceInfo();
                BeanUtils.copyProperties(namespace, overviewNSInfo);
                // 累计集群的注册命名空间数
                overviewClusterInfo.setRegNamespaceCount(overviewClusterInfo.getRegNamespaceCount() + 1);
                // 初始化为0，累加namespace数
                overview.setTotalNamespaceCount(overview.getTotalNamespaceCount() + 1);
                overviewClusterInfo.getNamespaces().add(overviewNSInfo);
                overviewNSInfo.setMiddlewares(new ArrayList<>());

                return overviewNSInfo;
            }).collect(Collectors.toMap(OverviewNamespaceInfo::getName, Function.identity()));

            List<Middleware> middlewareInfoDTOList = middlewareCRService.list(clusterDTO.getId(), null, null, true);
            //获取chartName对应的chartVersion
            List<HelmListInfo> helmListInfoList = helmChartService.listHelm("", "", clusterDTO);
            Map<String, List<HelmListInfo>> helmListMap =
                    helmListInfoList.stream().collect(Collectors.groupingBy(HelmListInfo::getNamespace));
            Map<String, Map<String, String>> chartVersionGroup = new HashMap<>();
            for (String key : helmListMap.keySet()){
                Map<String, String> chartVersionMap = helmListMap.get(key).stream().collect(Collectors.toMap(HelmListInfo::getName , helmInfo -> {
                    String chart = helmInfo.getChart();
                    return chart.substring(chart.indexOf(LINE) + 1);
                }));
                chartVersionGroup.put(key, chartVersionMap);
            }
            //获取imagePath
            List<BeanMiddlewareInfo> mwInfoList = middlewareInfoService.list(true);
            Map<String, String> imagePathMap = mwInfoList.stream().collect(Collectors.toMap(
                    mwInfo -> mwInfo.getChartName() + LINE + mwInfo.getChartVersion(), BeanMiddlewareInfo::getImagePath));

            if (CollectionUtils.isEmpty(middlewareInfoDTOList)) {
                return;
            }
            middlewareInfoDTOList.forEach(middleware -> {
                // 不在注册命名空间里就出来
                if (!namespaceMap.containsKey(middleware.getNamespace())) {
                    return;
                }
                boolean hasCPUInNSQuota = false;
                OverviewNamespaceInfo overviewNSInfo = namespaceMap.get(middleware.getNamespace());

                // 先检查分区的ResourceQuota是否有配额，没有的话采用所有中间件的配额使用量
                Map<String, List<String>> resourceQuota
                        = resourceQuotaService.list(overviewNSInfo.getClusterId(), overviewNSInfo.getName());
                hasCPUInNSQuota = hasCPUInQuota(resourceQuota);
                if (hasCPUInNSQuota) {
                    overviewNSInfo.setCpu(Double.parseDouble(resourceQuota.get(CPU).get(1)));
                    overviewNSInfo.setMemory(Double.parseDouble(resourceQuota.get(MEMORY).get(1)));
                }

                // 累计命名空间实例数
                overviewNSInfo.setInstanceCount(overviewNSInfo.getInstanceCount() + 1);
                // 累计集群实例数
                overviewClusterInfo.setInstanceCount(overviewClusterInfo.getInstanceCount() + 1);
                // 累计全局总实例数
                overview.setTotalInstanceCount(overview.getTotalInstanceCount() + 1);
                OverviewInstanceInfo overviewInstanceInfo = new OverviewInstanceInfo();
                overviewInstanceInfo.setClusterId(clusterDTO.getId());
                overviewInstanceInfo.setNamespace(middleware.getNamespace());
                overviewInstanceInfo.setName(middleware.getName());
                overviewInstanceInfo.setType(middleware.getType());
                overviewInstanceInfo.setChartName(middleware.getType());
                overviewInstanceInfo.setChartVersion(chartVersionGroup
                        .getOrDefault(middleware.getNamespace(), new HashMap<>()).getOrDefault(middleware.getName(), ""));
                overviewInstanceInfo.setImagePath(imagePathMap.getOrDefault(
                        overviewInstanceInfo.getChartName() + LINE + overviewInstanceInfo.getChartVersion(), ""));
                MiddlewareQuota quota = middleware.getQuota().get(middleware.getType());

                overviewInstanceInfo.setNodeCount(quota.getNum());

                if (!NameConstant.RUNNING.equalsIgnoreCase(middleware.getStatus())) {
                    overviewInstanceInfo.setStatus(false);
                }
                // 累计异常实例数指标，并把异常状态向上传递
                if (BooleanUtils.isFalse(overviewInstanceInfo.getStatus())) {
                    overview.setTotalExceptionCount(overview.getTotalExceptionCount() + 1);
                    overviewNSInfo.setInstanceExceptionCount(overviewNSInfo.getInstanceExceptionCount() + 1);
                    // 分区的异常状态被设置过了，就不用再次设置分区和集群的了，避免重复设置
                    if (BooleanUtils.isNotFalse(overviewNSInfo.getStatus())) {
                        overviewNSInfo.setStatus(false);
                        overviewClusterInfo.setStatus(false);
                    }
                }
                double cpu = ResourceCalculationUtil
                        .getResourceValue(middleware.getQuota().get(middleware.getType()).getCpu(), CPU, "");
                double memory = ResourceCalculationUtil.getResourceValue(
                        middleware.getQuota().get(middleware.getType()).getMemory(), NameConstant.MEMORY,
                        ResourceUnitEnum.G.getUnit());
                overviewInstanceInfo.setTotalCpu(cpu);
                overviewInstanceInfo.setTotalMemory(memory);

                // 如果分区上没有设置配额（ResourceQuota），则使用实例的配额累加
                if (!hasCPUInNSQuota) {
                    overviewNSInfo.setCpu(mathAdd(overviewNSInfo.getCpu(), overviewInstanceInfo.getTotalCpu()));
                    overviewNSInfo.setMemory(mathAdd(overviewNSInfo.getMemory(), overviewInstanceInfo.getTotalMemory()));
                }
                overviewNSInfo.getMiddlewares().add(overviewInstanceInfo);


            });

        });

        return overview;
    }

    /**
     * 判断ResourceQuota是否有cpu配额
     *
     * @param resourceQuota
     * @return
     */
    private boolean hasCPUInQuota(Map<String, List<String>> resourceQuota) {
        try {
            // cpu值列表第二个是配额
            return !CollectionUtils.isEmpty(resourceQuota)
                    && resourceQuota.containsKey(CPU)
                    && Double.parseDouble(resourceQuota.get(CPU).get(1)) > 0.0d;
        } catch (Exception e) {
            log.error(e.getMessage());
            return false;
        }

    }

    private static Double mathAdd(Double a, Double b) {
        BigDecimal b1 = new BigDecimal(Double.toString(null == a ? 0.0d : a));
        BigDecimal b2 = new BigDecimal(Double.toString(null == b ? 0.0d : b));
        return b1.add(b2).doubleValue();
    }

    private void changeType(BeanAlertRecord record,AlertDTO alertDTO) {
        if (StringUtils.isNotEmpty(record.getType())) {
            if ("mysql".equals(record.getType())) {
                alertDTO.setCapitalType("MySQL");
            }
            if ("elasticsearch".equals(record.getType())) {
                alertDTO.setCapitalType("Elasticsearch");
            }
            if ("kafka".equals(record.getType())) {
                alertDTO.setCapitalType("Kafka");
            }
            if ("redis".equals(record.getType())) {
                alertDTO.setCapitalType("Redis");
            }
            if ("rocketmq".equals(record.getType())) {
                alertDTO.setCapitalType("RocketMQ");
            }
            if ("zookeeper".equals(record.getType())) {
                alertDTO.setCapitalType("ZooKeeper");
            }
        }

    }

    @Override
    public List<MiddlewareDTO> getListPlatformOverview() {
        //获取所有集群
        List<MiddlewareClusterDTO> clusterDTOS = clusterService.listClusters();
        List<MiddlewareDTO> middlewareDTOList = new ArrayList<>();
        clusterDTOS.forEach(clusterDTO -> {
            //获取所有分区namespaces
            List<Namespace> namespaces = namespaceService.list(clusterDTO.getId(), true, true, true, null);
            if (CollectionUtils.isEmpty(namespaces)) {
                return;
            }

            //获取已注册分区
            List<Namespace> registeredNamespace = namespaces.stream().filter(namespace -> namespace.isRegistered()).collect(Collectors.toList());
            registeredNamespace.stream().forEach(namespace -> {
                //获取分区下所有实例
                List<MiddlewareCRD> middlewareCRDS = middlewareCRService.listCR(clusterDTO.getId(), namespace.getName(), null);
                Map<String, List<String>> quotas = namespace.getQuotas();

                String namespaceCpu = null;
                String namespaceMemory = null;
                if (quotas != null) {
                    List<String> cpuList = quotas.get("cpu");
                    if (!CollectionUtils.isEmpty(cpuList)) {
                        namespaceCpu = cpuList.get(2) + "/" + cpuList.get(1);
                    }
                    List<String> memoryList = quotas.get("memory");
                    if (!CollectionUtils.isEmpty(memoryList)) {
                        namespaceMemory = memoryList.get(2) + "/" + memoryList.get(1);
                    }
                }

                for (MiddlewareCRD middlewareCRD : middlewareCRDS) {
                    MiddlewareDTO middlewareDTO = new MiddlewareDTO();
                    middlewareDTO.setClusterName(clusterDTO.getName());
                    middlewareDTO.setClusterId(clusterDTO.getId());
                    middlewareDTO.setNamespace(namespace.getName());
                    middlewareDTO.setNamespaceCpu(namespaceCpu);
                    middlewareDTO.setNamespaceMemory(namespaceMemory);

                    MiddlewareSpec spec = middlewareCRD.getSpec();
                    if (spec != null) {
                        middlewareDTO.setType(MiddlewareTypeEnum.findTypeByCrdType(spec.getType()));
                        Middleware detail = middlewareService.detail(clusterDTO.getId(), namespace.getName(), spec.getName(), middlewareDTO.getType());
                        Map<String, MiddlewareQuota> quota = detail.getQuota();
                        if (quota != null && quota.get(middlewareDTO.getType()) != null) {
                            MiddlewareQuota middlewareQuota = quota.get(middlewareDTO.getType());
                            middlewareDTO.setCpu(middlewareQuota.getCpu());
                            middlewareDTO.setMemory(middlewareQuota.getMemory().replace("Gi", ""));
                        }
                        middlewareDTO.setChartVersion(detail.getChartVersion());
                        middlewareDTO.setName(spec.getName());
                        //mysql实例判断是否是备实例
                        if (MiddlewareTypeEnum.MYSQL.getType().equals(MiddlewareTypeEnum.findTypeByCrdType(spec.getType()))) {
                            MysqlDTO mysqlDTO = detail.getMysqlDTO();
                            if (mysqlDTO != null && mysqlDTO.getIsSource() != null) {
                                middlewareDTO.setSource(mysqlDTO.getIsSource());
                            }
                        } else {
                            middlewareDTO.setName(spec.getName());
                        }
                    }

                    MiddlewareStatus status = middlewareCRD.getStatus();
                    if (status != null && status.getResources() != null) {
                        middlewareDTO.setStatus(status.getPhase());
                        String creationTimestamp = status.getCreationTimestamp();
                        String createTime = DateUtil.utc2Local(creationTimestamp, DateType.YYYY_MM_DD_T_HH_MM_SS_Z.getValue(), DateType.YYYY_MM_DD_HH_MM_SS.getValue());
                        middlewareDTO.setCreateTime(createTime);
                        middlewareDTO.setCreateDate(DateUtil.StringToDate(creationTimestamp, DateType.YYYY_MM_DD_T_HH_MM_SS_Z));
                    }
                    middlewareDTOList.add(middlewareDTO);
                }
            });
        });
        Collections.sort(middlewareDTOList);
        return middlewareDTOList;
    }

    @Override
    public PlatformOverviewDTO getClusterPlatformOverview(String clusterId) {
        List<MiddlewareClusterDTO> clusterList = clusterService.listClusters(true, null);
        if (StringUtils.isNotBlank(clusterId)) {
            clusterList = clusterList.stream().filter(cluster -> cluster.getId().equals(clusterId)).collect(Collectors.toList());
        }
        PlatformOverviewDTO platformOverviewDTO = new PlatformOverviewDTO();
        platformOverviewDTO.setZeusVersion(version);

        //获取集群已注册分区
        List<Namespace> registeredNamespaceList = clusterService.getRegisteredNamespaceNum(clusterList);
        //获取集群CPU和内存的配额信息
        ClusterQuotaDTO clusterQuota = clusterService.getClusterQuota(clusterList);
        clusterQuota.setClusterNum(clusterList.size());
        clusterQuota.setNamespaceNum(registeredNamespaceList.size());
        platformOverviewDTO.setClusterQuota(clusterQuota);

        //获取控制器状态信息
        MiddlewareOperatorDTO operatorInfo = middlewareInfoService.getOperatorInfo(clusterList);
        platformOverviewDTO.setOperatorDTO(operatorInfo);

        //获取异常告警信息
        Date now = DateUtil.addHour(new Date(), -6);
        Date ago = DateUtil.addHour(now, -24);
        String beginTime = DateUtils.DateToString(ago, DateType.YYYY_MM_DD_HH_MM_SS.getValue());
        String endTime = DateUtils.DateToString(now, DateType.YYYY_MM_DD_HH_MM_SS.getValue());
        List<String> hourList = DateUtil.calcHour(now);
        List<Map<String, Object>> criticalList = beanAlertRecordMapper.queryByTimeAndLevel(beginTime, endTime, "critical");
        List<Map<String, Object>> infoList = beanAlertRecordMapper.queryByTimeAndLevel(beginTime, endTime, "info");
        List<Map<String, Object>> warningList = beanAlertRecordMapper.queryByTimeAndLevel(beginTime, endTime, "warning");

        AlertSummaryDTO alertSummaryDTO = new AlertSummaryDTO();
        alertSummaryDTO.setCriticalList(AlertDataUtil.checkAndFillZero(criticalList, hourList));
        alertSummaryDTO.setInfoList(AlertDataUtil.checkAndFillZero(infoList, hourList));
        alertSummaryDTO.setWarningList(AlertDataUtil.checkAndFillZero(warningList, hourList));
        alertSummaryDTO.setCriticalSum(AlertDataUtil.countAlertNum(criticalList));
        alertSummaryDTO.setInfoSum(AlertDataUtil.countAlertNum(infoList));
        alertSummaryDTO.setWarningSum(AlertDataUtil.countAlertNum(warningList));
        platformOverviewDTO.setAlertSummary(alertSummaryDTO);

        //获取审计信息
        List<BeanOperationAudit> auditList = operationAuditService.listRecent(20);
        platformOverviewDTO.setAuditList(auditList);
        return platformOverviewDTO;
    }

    @Override
    public PlatformOverviewDTO getClusterMiddlewareInfo(String clusterId) {
        PlatformOverviewDTO platformOverviewDTO = new PlatformOverviewDTO();
        List<MiddlewareClusterDTO> clusterList = null;
        try {
            clusterList = clusterService.listClusters(true, null);
        } catch (BusinessException e) {
            log.error("集群列表查询失败", e);
        }
        if (CollectionUtils.isEmpty(clusterList)) {
            platformOverviewDTO.setBriefInfoList(new ArrayList<>());
            return platformOverviewDTO;
        }
        if (StringUtils.isNotBlank(clusterId)) {
            clusterList = clusterList.stream().filter(cluster -> cluster.getId().equals(clusterId)).collect(Collectors.toList());
        }
        List<MiddlewareBriefInfoDTO> middlewareBriefInfoList = middlewareService.getMiddlewareBriefInfoList(clusterList);
        // 多集群时，合并相同中间件数量信息
        if (StringUtils.isBlank(clusterId) && clusterList.size() > 1) {
            Map<String, MiddlewareBriefInfoDTO> resMap = new HashMap<>();
            middlewareBriefInfoList.forEach(mwInfo -> {
                MiddlewareBriefInfoDTO briefInfoDTO = resMap.get(mwInfo.getName());
                if (briefInfoDTO != null) {
                    briefInfoDTO.setServiceNum(briefInfoDTO.getServiceNum() + mwInfo.getServiceNum());
                    briefInfoDTO.setErrServiceNum(briefInfoDTO.getErrServiceNum() + mwInfo.getErrServiceNum());
                } else {
                    resMap.put(mwInfo.getName(), mwInfo);
                }
            });
            middlewareBriefInfoList.clear();
            middlewareBriefInfoList.addAll(resMap.values());
        }
        platformOverviewDTO.setBriefInfoList(middlewareBriefInfoList);
        return platformOverviewDTO;
    }

}
