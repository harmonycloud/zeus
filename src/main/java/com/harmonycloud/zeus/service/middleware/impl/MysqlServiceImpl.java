package com.harmonycloud.zeus.service.middleware.impl;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.zeus.operator.api.MysqlOperator;
import com.harmonycloud.zeus.service.k8s.IngressService;
import com.harmonycloud.zeus.service.k8s.impl.ServiceServiceImpl;
import com.harmonycloud.zeus.service.middleware.MysqlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
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
                Middleware relationMiddleware = null;
                try {
                    relationMiddleware = middlewareService.detail(relationClusterId, relationNamespace, relationName, MiddlewareTypeEnum.MYSQL.getType());
                } catch (Exception e) {
                    log.error("关联实例不存在", e);
                    return BaseResult.ok(res);
                }
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

    public JSONObject queryAllAccessInfo(String clusterId, String namespace, String middlewareName, Boolean isSource) {
        List<IngressDTO> ingressDTOS = ingressService.get(clusterId, namespace, MiddlewareTypeEnum.MYSQL.name(), middlewareName);
        ingressDTOS = ingressDTOS.stream().filter(ingressDTO -> (
                !ingressDTO.getName().contains("readonly") && ingressDTO.getExposeType().equals(MIDDLEWARE_EXPOSE_NODEPORT))
        ).collect(Collectors.toList());

        JSONObject mysqlInfo = new JSONObject();
        if (!CollectionUtils.isEmpty(ingressDTOS)) {
            // 优先使用ingress或NodePort暴露的服务
            IngressDTO ingressDTO = ingressDTOS.get(0);
            String exposeIP = ingressDTO.getExposeIP();
            List<ServiceDTO> serviceList = ingressDTO.getServiceList();
            if (!CollectionUtils.isEmpty(serviceList)) {
                ServiceDTO serviceDTO = serviceList.get(0);
                String exposePort = serviceDTO.getExposePort();
                mysqlInfo.put("address", exposeIP + ":" + exposePort + " (集群外部)");
            }
        }else{
            // 没有暴露对外服务，则使用集群内服务
            ServicePortDTO servicePortDTO = serviceService.get(clusterId, namespace, middlewareName);
            if (servicePortDTO != null && !CollectionUtils.isEmpty(servicePortDTO.getPortDetailDtoList())) {
                mysqlInfo.put("address", servicePortDTO.getClusterIP() + ":" + servicePortDTO.getPortDetailDtoList().get(0).getTargetPort() + "(集群内部)");
            } else {
                mysqlInfo.put("address", "无");
            }
        }
        mysqlInfo.put("username", "root");
        return mysqlInfo;
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
