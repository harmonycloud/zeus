package com.middleware.zeus.service.system.impl;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.constants.NameConstant;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.ServicePort;
import com.middleware.zeus.integration.cluster.MysqlReplicateWrapper;
import com.middleware.zeus.integration.cluster.bean.MysqlReplicateCR;
import com.middleware.zeus.integration.platform.PlatformClient;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.service.system.PlatformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

/**
 * @auther wangpenglei
 * @date 2023/3/22 10:35
 */
@Service
@Slf4j
public class PlatformServiceImpl implements PlatformService {

    @Autowired
    private HelmChartService helmChartService;

    @Autowired
    private MysqlReplicateWrapper mysqlReplicateWrapper;

    @Autowired
    private PlatformClient platformClient;


    @Override
    public JSONObject queryAccessInfo() {
        JSONObject values = helmChartService.getZeusMysqlInstallValues();
        JSONObject res = new JSONObject();
        JSONObject args = values.getJSONObject("args");
        Boolean isSource = args.getBoolean("isSource");
        res.put("isSource", isSource);
        if (isSource == null || !isSource){
            return res;
        }
        JSONObject chief = args.getJSONObject("chief");
        if (chief != null) {
            res.put("chief", chief);
        }
        JSONObject relation = args.getJSONObject("relation");
        if (relation != null){
            res.put("relation", relation);
        }
        Boolean isSwitched = args.getBoolean("isSwitched");
        if (isSwitched != null && isSwitched){
            res.put("isSwitched",true);
        } else {
            res.put("isSwitched",false);
        }

        // TODO 获取主备平台健康状态

        return res;
    }

    @Override
    public void saveRelationAddr(ServicePort servicePort, String relationName) {
        JSONObject values = helmChartService.getZeusMysqlInstallValues();
        JSONObject newValues = JSONObject.parseObject(values.toJSONString());
        JSONObject relation = newValues.getJSONObject("args").getJSONObject("relation");
        relation.put("protocol", servicePort.getProtocol());
        relation.put("host", servicePort.getPort());
        relation.put("port", servicePort.getPort());
        relation.put("name", relationName);
        helmChartService.upgradeZeusMysql(values,newValues);
    }

    @Override
    public void saveChiefAddr(ServicePort servicePort, String chiefName) {
        JSONObject values = helmChartService.getZeusMysqlInstallValues();
        JSONObject newValues = JSONObject.parseObject(values.toJSONString());
        JSONObject chief = newValues.getJSONObject("args").getJSONObject("chief");
        chief.put("chiefProtocol", servicePort.getProtocol());
        chief.put("chiefHost", servicePort.getPort());
        chief.put("chiefPort", servicePort.getPort());
        chief.put("chiefName", chiefName);
        helmChartService.upgradeZeusMysql(values,newValues);
    }

    @Override
    public void switchPlatform(HttpServletRequest request) throws IOException {
        JSONObject values = helmChartService.getZeusMysqlInstallValues();
        JSONObject newValues = JSONObject.parseObject(values.toJSONString());
        Boolean isSource = values.getJSONObject("args").getBoolean("isSource");
        if (isSource == null){
            throw new BusinessException(ErrorMessage.SWITCH_PLATFORM_NOT_SUPPORT);
        }
        if (isSource){
            JSONObject res = platformClient.switchPlatform(request.getHeader("userToken"));
            if (res != null && res.getJSONObject("data").getBoolean("success")){
                log.info("切换成功，res = {}",res);
                newValues.put("type","master-slave");
                helmChartService.upgradeZeusMysql(values,newValues);
            }
        }else{
            MysqlReplicateCR mr = mysqlReplicateWrapper.getMysqlReplicate(NameConstant.ZEUS, NameConstant.ZEUS_MYSQL_REPLICATE);
            mr.getSpec().setEnable(false);
            mysqlReplicateWrapper.updateMysqlReplicate(mr);
            JSONObject args = newValues.getJSONObject("args");
            args.put("isSource",true);
            args.put("isSwitched",true);
            newValues.put("type","master-slave");
            helmChartService.upgradeZeusMysql(values,newValues);
        }
    }


}
