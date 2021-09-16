package com.harmonycloud.zeus.service.middleware.impl;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.constants.CommonConstant;
import com.harmonycloud.caas.common.model.MiddlewareServiceNameIndex;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareInfo;
import com.harmonycloud.zeus.integration.cluster.bean.MysqlReplicateCRD;
import com.harmonycloud.zeus.integration.cluster.bean.MysqlReplicateSpec;
import com.harmonycloud.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.harmonycloud.zeus.schedule.MiddlewareManageTask;
import com.harmonycloud.zeus.service.k8s.*;
import com.harmonycloud.zeus.service.log.EsComponentService;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.tool.excel.ExcelUtil;
import com.harmonycloud.tool.page.PageObject;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCRD;
import com.harmonycloud.zeus.util.ServiceNameConvertUtil;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.zeus.operator.BaseOperator;
import com.harmonycloud.zeus.service.AbstractBaseService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;

import lombok.extern.slf4j.Slf4j;

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
        List<BeanMiddlewareInfo> mwInfoList = middlewareInfoService.list();
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
    public String getManagePlatformAddress(Middleware middleware, String clusterId) {
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
                return exposeIP + ":" + exposePort;
            }
        }
        return "";
    }

    @Override
    public List listAllMiddleware(String clusterId, String namespace,String keyword) {
        List<MiddlewareInfoDTO> middlewareInfoDTOList = middlewareInfoService.list(clusterId);

        List<Map<String, Object>> serviceList = new ArrayList<>();
        middlewareInfoDTOList.forEach(middlewareInfoDTO -> {
            List<Middleware> middlewareServiceList = simpleList(clusterId, namespace, middlewareInfoDTO.getChartName(), keyword);
            middlewareServiceList.forEach(middleware -> {
                MiddlewareCRD middlewareCRD = middlewareCRDService.getCR(clusterId, namespace, middlewareInfoDTO.getType(), middleware.getName());
                if (middlewareCRD != null && middlewareCRD.getStatus() != null && middlewareCRD.getStatus().getInclude() != null) {
                    List<MiddlewareInfo> middlewareInfos = middlewareCRD.getStatus().getInclude().get(PODS);
                    middleware.setPodNum(middlewareInfos.size());
                    if (middleware.getManagePlatform() != null && middleware.getManagePlatform()) {
                        String managePlatformAddress = getManagePlatformAddress(middleware, clusterId);
                        middleware.setManagePlatformAddress(managePlatformAddress);
                    }
                }
            });

            Map<String, Object> middlewareMap = new HashMap<>();
            middlewareMap.put("name", middlewareInfoDTO.getChartName());
            middlewareMap.put("image", middlewareInfoDTO.getImage());
            middlewareMap.put("imagePath", middlewareInfoDTO.getImagePath());
            middlewareMap.put("chartName", middlewareInfoDTO.getChartName());
            middlewareMap.put("chartVersion", middlewareInfoDTO.getChartVersion());
            middlewareMap.put("version", middlewareInfoDTO.getVersion());
            middlewareMap.put("serviceList", middlewareServiceList);
            middlewareMap.put("serviceNum", middlewareServiceList.size());
            serviceList.add(middlewareMap);
        });
        Collections.sort(serviceList, new ServiceMapComparator());

        return serviceList;
    }

    /**
     * 服务排序类，按服务数量进行排序
     */
    public static class ServiceMapComparator implements Comparator<Map> {
        @Override
        public int compare(Map service1, Map service2) {
            if (Integer.parseInt(service1.get("serviceNum").toString()) > Integer.parseInt(service2.get("serviceNum").toString())) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
