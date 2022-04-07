package com.harmonycloud.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.constants.CommonConstant;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.model.MysqlAccessInfo;
import com.harmonycloud.caas.common.model.MysqlDbDTO;
import com.harmonycloud.caas.common.model.MysqlUserDTO;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.tool.excel.ExcelUtil;
import com.harmonycloud.tool.page.PageObject;
import com.harmonycloud.zeus.bean.BeanMysqlUser;
import com.harmonycloud.zeus.operator.api.MysqlOperator;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.IngressService;
import com.harmonycloud.zeus.service.k8s.impl.ServiceServiceImpl;
import com.harmonycloud.zeus.service.log.EsComponentService;
import com.harmonycloud.zeus.service.middleware.MysqlService;
import com.harmonycloud.zeus.service.mysql.MysqlDbService;
import com.harmonycloud.zeus.service.mysql.MysqlUserService;
import com.harmonycloud.zeus.util.MyAESUtil;
import com.harmonycloud.zeus.util.MysqlConnectionUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.*;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_EXPOSE_NODEPORT;

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
    private ClusterService clusterService;
    @Autowired
    private EsComponentService esComponentService;
    @Autowired
    private MysqlUserService mysqlUserService;

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
    public BaseResult queryAccessInfo(String clusterId, String namespace, String middlewareName) {
        // 获取对外访问信息
        Middleware middleware = middlewareService.detail(clusterId, namespace, middlewareName, MiddlewareTypeEnum.MYSQL.getType());
        JSONObject res = new JSONObject();
        MysqlDTO mysqlDTO = middleware.getMysqlDTO();
        if (mysqlDTO != null) {
            Boolean isSource = mysqlDTO.getIsSource();
            MysqlAccessInfo source = queryBasicAccessInfo(clusterId, namespace, middlewareName, middleware);
            source.setPassword(middleware.getPassword());
            source.setClusterId(clusterId);
            source.setNamespace(namespace);
            source.setMiddlewareName(middlewareName);
            res.put(getInstanceType(isSource, mysqlDTO.getOpenDisasterRecoveryMode()), source);
            if (isSource != null && mysqlDTO.getOpenDisasterRecoveryMode() != null && mysqlDTO.getOpenDisasterRecoveryMode()) {
                String relationClusterId = mysqlDTO.getRelationClusterId();
                String relationNamespace = mysqlDTO.getRelationNamespace();
                String relationName = mysqlDTO.getRelationName();
                Middleware relationMiddleware;
                try {
                    relationMiddleware = middlewareService.detail(relationClusterId, relationNamespace, relationName, MiddlewareTypeEnum.MYSQL.getType());
                } catch (Exception e) {
                    log.error("关联实例不存在", e);
                    return BaseResult.ok(res);
                }
                MysqlDTO relationMiddlewareMysqlDTO = relationMiddleware.getMysqlDTO();
                MysqlAccessInfo relation;
                if (relationMiddlewareMysqlDTO != null) {
                    relation = queryBasicAccessInfo(relationClusterId, relationNamespace, relationName, middleware);
                } else {
                    relation = queryBasicAccessInfo(relationClusterId, relationNamespace, relationName, middleware);
                }
                relation.setPassword( relationMiddleware.getPassword());
                relation.setClusterId( relationClusterId);
                relation.setNamespace(relationNamespace);
                relation.setMiddlewareName( relationName);
                res.put(getInstanceType(!isSource, relationMiddleware.getMysqlDTO().getOpenDisasterRecoveryMode()), relation);
            }
        }
        return BaseResult.ok(res);
    }

    @Override
    public PageObject<MysqlLogDTO> slowsql(MysqlLogQuery slowLogQuery) throws Exception {
        MiddlewareClusterDTO cluster = clusterService.findById(slowLogQuery.getClusterId());
        PageObject<MysqlLogDTO> slowSqlDTOS = esComponentService.getSlowSql(cluster, slowLogQuery);
        return slowSqlDTOS;
    }

    @Override
    public void slowsqlExcel(MysqlLogQuery slowLogQuery, HttpServletResponse response, HttpServletRequest request) throws Exception {
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
    public PageObject<MysqlLogDTO> auditSql(MysqlLogQuery auditLogQuery) {
        MiddlewareClusterDTO cluster = clusterService.findById(auditLogQuery.getClusterId());
        PageObject<MysqlLogDTO> slowSqlDTOS = null;
        try {
            slowSqlDTOS = esComponentService.getAuditSql(cluster, auditLogQuery);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return slowSqlDTOS;
    }

    public MysqlAccessInfo queryBasicAccessInfo(String clusterId, String namespace, String middlewareName, Middleware middleware) {
        if (middleware == null) {
            middleware = middlewareService.detail(clusterId, namespace, middlewareName, MiddlewareTypeEnum.MYSQL.getType());
        }
        List<IngressDTO> ingressDTOS = ingressService.get(clusterId, namespace, MiddlewareTypeEnum.MYSQL.name(), middlewareName);
        ingressDTOS = ingressDTOS.stream().filter(ingressDTO -> (
                !ingressDTO.getName().contains("readonly"))
        ).collect(Collectors.toList());

        MysqlAccessInfo mysqlAccessInfo = new MysqlAccessInfo();
        if (!CollectionUtils.isEmpty(ingressDTOS)) {
            // 优先使用ingress或NodePort暴露的服务
            IngressDTO ingressDTO = ingressDTOS.get(0);
            String exposeIP = ingressDTO.getExposeIP();
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
        setPassword(middleware, mysqlAccessInfo);
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

    public MysqlAccessInfo getAccessInfo(MysqlUserDTO user) {
        return queryBasicAccessInfo(user.getClusterId(), user.getNamespace(), user.getMiddlewareName(), null);
    }

    public MysqlAccessInfo getAccessInfo(MysqlDbDTO db) {
        return queryBasicAccessInfo(db.getClusterId(), db.getNamespace(), db.getMiddlewareName(), null);
    }

    public MysqlAccessInfo getAccessInfo(String clusterId, String namespace, String middlewareName) {
        return queryBasicAccessInfo(clusterId, namespace, middlewareName, null);
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
