package com.harmonycloud.zeus.service.middleware.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.zeus.service.k8s.IngressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.zeus.operator.api.MysqlOperator;
import com.harmonycloud.zeus.service.middleware.AbstractMiddlewareService;
import com.harmonycloud.zeus.service.middleware.MysqlService;
import org.springframework.util.CollectionUtils;

import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_EXPOSE_NODEPORT;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Slf4j
@Service
public class MysqlServiceImpl extends AbstractMiddlewareService implements MysqlService {

    @Autowired
    private MysqlOperator mysqlOperator;
    @Autowired
    private IngressService ingressService;
    @Autowired
    private MiddlewareServiceImpl middlewareService;

    @Override
    public List<MysqlBackupDto> listBackups(String clusterId, String namespace, String middlewareName) {
        Middleware middleware = new Middleware(clusterId, namespace, middlewareName, MiddlewareTypeEnum.MYSQL.getType());
        return mysqlOperator.listBackups(middleware);
    }

    @Override
    public ScheduleBackupConfig getScheduleBackups(String clusterId, String namespace, String middlewareName) {
        Middleware middleware = new Middleware(clusterId, namespace, middlewareName, MiddlewareTypeEnum.MYSQL.getType());
        return mysqlOperator.getScheduleBackupConfig(middleware);
    }

    @Override
    public void createScheduleBackup(String clusterId, String namespace, String middlewareName,Integer keepBackups, String cron) {
        Middleware middleware = new Middleware(clusterId, namespace, middlewareName, MiddlewareTypeEnum.MYSQL.getType());
        mysqlOperator.createScheduleBackup(middleware, keepBackups, cron);
    }

    @Override
    public void createBackup(String clusterId, String namespace, String middlewareName) {
        Middleware middleware = new Middleware(clusterId, namespace, middlewareName, MiddlewareTypeEnum.MYSQL.getType());
        mysqlOperator.createBackup(middleware);
    }

    @Override
    public void deleteBackup(String clusterId, String namespace, String middlewareName, String backupFileName, String backupName) throws Exception{
        Middleware middleware = new Middleware(clusterId, namespace, middlewareName, MiddlewareTypeEnum.MYSQL.getType());
        mysqlOperator.deleteBackup(middleware, backupFileName, backupName);
    }

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
            JSONObject source = queryAllAccessInfo(clusterId, namespace, middlewareName, isSource);
            source.put("password", middleware.getPassword());
            source.put("clusterId", clusterId);
            source.put("namespace", namespace);
            source.put("middlewareName", middlewareName);
            res.put(getInstanceType(isSource, mysqlDTO.getOpenDisasterRecoveryMode()), source);
            if (isSource != null && mysqlDTO.getOpenDisasterRecoveryMode() != null && mysqlDTO.getOpenDisasterRecoveryMode()) {
                String relationClusterId = mysqlDTO.getRelationClusterId();
                String relationNamespace = mysqlDTO.getRelationNamespace();
                String relationName = mysqlDTO.getRelationName();
                Middleware relationMiddleware = middlewareService.detail(relationClusterId, relationNamespace, relationName, MiddlewareTypeEnum.MYSQL.getType());
                MysqlDTO relationMiddlewareMysqlDTO = relationMiddleware.getMysqlDTO();
                JSONObject relation;
                if (relationMiddlewareMysqlDTO != null) {
                    relation = queryAllAccessInfo(relationClusterId, relationNamespace, relationName, relationMiddlewareMysqlDTO.getIsSource());
                } else {
                    relation = queryAllAccessInfo(relationClusterId, relationNamespace, relationName, null);
                }
                relation.put("password", relationMiddleware.getPassword());
                relation.put("clusterId", relationClusterId);
                relation.put("namespace", relationNamespace);
                relation.put("middlewareName", relationName);
                res.put(getInstanceType(!isSource, relationMiddleware.getMysqlDTO().getOpenDisasterRecoveryMode()), relation);
            }
        }
        return BaseResult.ok(res);
    }

    public JSONObject queryAllAccessInfo(String clusterId, String namespace, String middlewareName,Boolean isSource) {
        String nodePortServiceName;
        if (isSource == null || isSource) {
            nodePortServiceName = middlewareName + "-nodeport";
        } else {
            nodePortServiceName = middlewareName + "-readonly-nodeport";
        }

        List<IngressDTO> ingressDTOS = ingressService.get(clusterId, namespace, MiddlewareTypeEnum.MYSQL.name(), middlewareName);
        List<IngressDTO> serviceDTOList = ingressDTOS.stream().filter(ingressDTO -> (
                ingressDTO.getName().equals(nodePortServiceName) && ingressDTO.getExposeType().equals(MIDDLEWARE_EXPOSE_NODEPORT))
        ).collect(Collectors.toList());

        JSONObject mysqlInfo = new JSONObject();
        if (!CollectionUtils.isEmpty(serviceDTOList)) {
            IngressDTO ingressDTO = serviceDTOList.get(0);
            String exposeIP = ingressDTO.getExposeIP();
            List<ServiceDTO> serviceList = ingressDTO.getServiceList();
            if (!CollectionUtils.isEmpty(serviceList)) {
                ServiceDTO serviceDTO = serviceList.get(0);
                String exposePort = serviceDTO.getExposePort();
                mysqlInfo.put("address", exposeIP + ":" + exposePort);
            }
            mysqlInfo.put("username", "root");
        }
        return mysqlInfo;
    }

    /**
     * 查询实例类型(是源实例还是灾备实例)
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
