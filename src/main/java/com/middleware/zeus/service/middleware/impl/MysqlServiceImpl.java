package com.middleware.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.constants.CommonConstant;
import com.middleware.zeus.common.enums.middleware.MiddlewareTypeEnum;
import com.middleware.zeus.common.model.MysqlAccessInfo;
import com.middleware.zeus.common.model.MysqlDRAccessInfo;
import com.middleware.zeus.common.model.MysqlDbDTO;
import com.middleware.zeus.common.model.MysqlUserDTO;
import com.middleware.zeus.util.date.DateUtils;
import com.middleware.zeus.util.excel.ExcelUtil;
import com.middleware.zeus.util.page.PageObject;
import com.middleware.zeus.common.model.middleware.*;
import com.middleware.zeus.bean.BeanMysqlUser;
import com.middleware.zeus.operator.api.MysqlOperator;
import com.middleware.zeus.service.k8s.ClusterService;
import com.middleware.zeus.service.k8s.IngressService;
import com.middleware.zeus.service.k8s.NodeService;
import com.middleware.zeus.service.k8s.impl.ServiceServiceImpl;
import com.middleware.zeus.service.log.EsComponentService;
import com.middleware.zeus.service.middleware.MysqlService;
import com.middleware.zeus.service.mysql.MysqlUserService;
import com.middleware.zeus.util.encrypt.MyAESUtil;
import com.middleware.zeus.util.middleware.MysqlConnectionUtil;
import com.skyview.language.annotations.TranslateAfterResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Slf4j
@Service
public class MysqlServiceImpl implements MysqlService {

    @Autowired
    private MysqlOperator mysqlOperator;
    @Autowired
    private IngressService ingressService;
    @Autowired
    private MiddlewareServiceImpl middlewareService;
    @Autowired
    private ServiceServiceImpl serviceService;
    @Autowired
    private EsComponentService esComponentService;
    @Autowired
    private MysqlUserService mysqlUserService;
    @Autowired
    private NodeService nodeService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private MysqlService mysqlService;

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
    public BaseResult switchDisasterRecovery(String clusterId, String namespace, String middlewareName) {
        try {
            mysqlOperator.switchDisasterRecovery(clusterId, namespace, middlewareName);
            return BaseResult.ok();
        } catch (Exception e) {
            log.error("灾备切换失败", e);
            return BaseResult.error();
        }
    }

    @Override
    public MysqlDRAccessInfo queryAccessInfo(String clusterId, String namespace, String middlewareName) {
        // 获取对外访问信息
        Middleware middleware = middlewareService.detail(clusterId, namespace, middlewareName, MiddlewareTypeEnum.MYSQL.getType());
        MysqlDRAccessInfo res = new MysqlDRAccessInfo();
        MysqlDTO mysqlDTO = middleware.getMysqlDTO();
        if (mysqlDTO != null) {
            Boolean isSource = mysqlDTO.getIsSource();
            MysqlAccessInfo source = mysqlService.queryBasicAccessInfo(clusterId, namespace, middlewareName, middleware);
            source.setClusterId(clusterId);
            source.setNamespace(namespace);
            source.setMiddlewareName(middlewareName);
            if (getInstanceType(isSource, mysqlDTO.getOpenDisasterRecoveryMode()).equals("source")) {
                res.setSource(source);
            } else {
                res.setDisasterRecovery(source);
            }
            if (isSource != null && mysqlDTO.getOpenDisasterRecoveryMode() != null && mysqlDTO.getOpenDisasterRecoveryMode()) {
                String relationClusterId = mysqlDTO.getRelationClusterId();
                String relationNamespace = mysqlDTO.getRelationNamespace();
                String relationName = mysqlDTO.getRelationName();
                Middleware relationMiddleware;
                try {
                    relationMiddleware = middlewareService.detail(relationClusterId, relationNamespace, relationName, MiddlewareTypeEnum.MYSQL.getType());
                } catch (Exception e) {
                    log.error("关联实例不存在", e);
                    return res;
                }
                MysqlAccessInfo relation;
                relation = mysqlService.queryBasicAccessInfo(relationClusterId, relationNamespace, relationName, middleware);
                relation.setClusterId( relationClusterId);
                relation.setNamespace(relationNamespace);
                relation.setMiddlewareName( relationName);
                if (getInstanceType(!isSource, relationMiddleware.getMysqlDTO().getOpenDisasterRecoveryMode()).equals("source")) {
                    res.setSource(source);
                } else {
                    res.setDisasterRecovery(source);
                }
            }
        }
        return res;
    }

    @Override
    public PageObject<MysqlLogDTO> slowsql(MiddlewareLogQuery slowLogQuery) throws Exception {
        PageObject<MysqlLogDTO> slowSqlDTOS = esComponentService.getSlowSql(slowLogQuery.getClusterId(), slowLogQuery);
        return slowSqlDTOS;
    }

    @Override
    public void slowsqlExcel(MiddlewareLogQuery slowLogQuery, HttpServletResponse response, HttpServletRequest request) throws Exception {
        slowLogQuery.setCurrent(1);
        slowLogQuery.setSize(CommonConstant.NUM_ONE_THOUSAND);
        PageObject<MysqlLogDTO> slowsql = slowsql(slowLogQuery);
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

    @Override
    public PageObject<MysqlLogDTO> auditSql(MiddlewareLogQuery auditLogQuery) {
        PageObject<MysqlLogDTO> slowSqlDTOS = null;
        try {
            slowSqlDTOS = esComponentService.getAuditSql(auditLogQuery.getClusterId(), auditLogQuery);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return slowSqlDTOS;
    }

    @Override
    @TranslateAfterResult
    public MysqlAccessInfo queryBasicAccessInfo(String clusterId, String namespace, String middlewareName, Middleware middleware) {
        if (middleware == null) {
            middleware = middlewareService.detail(clusterId, namespace, middlewareName, MiddlewareTypeEnum.MYSQL.getType());
        }
        List<IngressDTO> serviceDTOS = ingressService.get(clusterId, namespace, MiddlewareTypeEnum.MYSQL.getType(), middlewareName);
        serviceDTOS = serviceDTOS.stream().filter(ingressDTO -> (
                !ingressDTO.getName().contains("readonly"))
        ).collect(Collectors.toList());

        MysqlAccessInfo mysqlAccessInfo = new MysqlAccessInfo();
        boolean withInCluster = clusterService.checkWithInCluster(clusterId);
        mysqlAccessInfo.setWithInCluster(withInCluster);
        if (!withInCluster && !CollectionUtils.isEmpty(serviceDTOS)) {
            // 优先使用ingress暴露的服务
            List<IngressDTO> ingressDTOS = serviceDTOS.stream().filter(ingressDTO ->
                    !StringUtils.isEmpty(ingressDTO.getIngressClassName())).collect(Collectors.toList());
            IngressDTO ingressDTO;
            String exposeIP = "";
            if (!CollectionUtils.isEmpty(ingressDTOS)) {
                ingressDTO = ingressDTOS.get(0);
                exposeIP = ingressService.getIngressIp(clusterId, ingressDTO.getIngressClassName());
            } else {
                ingressDTO = serviceDTOS.get(0);
                exposeIP = nodeService.getAvailableNodeIP(clusterId);
            }
            List<ServiceDTO> serviceList = ingressDTO.getServiceList();
            if (!CollectionUtils.isEmpty(serviceList)) {
                ServiceDTO serviceDTO = serviceList.get(0);
                String exposePort = serviceDTO.getExposePort();
                mysqlAccessInfo.setAddress(exposeIP + ":" + exposePort + " (集群外部)");
                mysqlAccessInfo.setHost(exposeIP);
                mysqlAccessInfo.setPort(exposePort);
            }
            mysqlAccessInfo.setOpenService(true);
        } else {
            // 没有暴露对外服务，则使用集群内服务
            ServicePortDTO servicePortDTO = serviceService.get(clusterId, namespace, middlewareName);
            if (servicePortDTO != null && !CollectionUtils.isEmpty(servicePortDTO.getPortDetailDtoList())) {
                mysqlAccessInfo.setAddress(servicePortDTO.getClusterIP() + ":" + servicePortDTO.getPortDetailDtoList().get(0).getTargetPort() + "(集群内部)");
                mysqlAccessInfo.setHost(servicePortDTO.getClusterIP());
                mysqlAccessInfo.setPort(servicePortDTO.getPortDetailDtoList().get(0).getTargetPort());
            } else {
                mysqlAccessInfo.setAddress("无");
            }
            mysqlAccessInfo.setOpenService(false);
        }
        mysqlAccessInfo.setUsername("root");
        mysqlAccessInfo.setClusterId(clusterId);
        mysqlAccessInfo.setNamespace(namespace);
        mysqlAccessInfo.setMiddlewareName(middlewareName);
        log.info("{}的访问信息:{}", middleware.getName(), mysqlAccessInfo);
        return mysqlAccessInfo;
    }

    /**
     * 设置mysql root 用户密码，默认从数据库中查，查不到再取values.yaml中的
     * @param middleware
     * @param mysqlAccessInfo
     */
    public void setPassword(Middleware middleware, MysqlAccessInfo mysqlAccessInfo) {
        BeanMysqlUser mysqlUser = mysqlUserService.select(MysqlConnectionUtil.getMysqlQualifiedName(middleware.getClusterId(), middleware.getNamespace(), middleware.getName()), "root");
        if (mysqlUser != null) {
            mysqlAccessInfo.setPassword(MyAESUtil.decodeBase64(mysqlUser.getPassword()));
        } else {
            mysqlAccessInfo.setPassword(middleware.getPassword());
        }
    }

    public MysqlAccessInfo checkAndGetDbManageAccessInfo(String clusterId, String namespace, String middlewareName) {
        MysqlAccessInfo mysqlAccessInfo = queryBasicAccessInfo(clusterId, namespace, middlewareName, null);
        if (mysqlAccessInfo.isOpenService() || mysqlAccessInfo.getWithInCluster()) {
            return mysqlAccessInfo;
        } else {
            Middleware middleware = new Middleware();
            middleware.setClusterId(clusterId);
            middleware.setNamespace(namespace);
            middleware.setName(middlewareName);
            middleware.setType(MiddlewareTypeEnum.MYSQL.getType());
            mysqlOperator.createOpenService(middleware, false, false);
            // ingress服务创建后不一定立即就可以使用，这里延迟5秒
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return queryBasicAccessInfo(clusterId, namespace, middlewareName, null);
        }
    }

    public MysqlAccessInfo getAccessInfo(MysqlUserDTO user) {
        return checkAndGetDbManageAccessInfo(user.getClusterId(), user.getNamespace(), user.getMiddlewareName());
    }

    public MysqlAccessInfo getAccessInfo(MysqlDbDTO db) {
        return checkAndGetDbManageAccessInfo(db.getClusterId(), db.getNamespace(), db.getMiddlewareName());
    }

    public MysqlAccessInfo getAccessInfo(String clusterId, String namespace, String middlewareName) {
        return checkAndGetDbManageAccessInfo(clusterId, namespace, middlewareName);
    }

    /**
     * 查询实例类型(是源实例还是灾备实例)
     *
     * @param isSource
     * @return
     */
    public String getInstanceType(Boolean isSource, Boolean openDisasterRecoveryMode) {
        if (openDisasterRecoveryMode == null || !openDisasterRecoveryMode) {
            return "source";
        }
        if (isSource == null || isSource) {
            return "source";
        } else {
            return "disasterRecovery";
        }
    }

}
