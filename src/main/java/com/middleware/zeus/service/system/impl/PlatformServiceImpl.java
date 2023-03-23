package com.middleware.zeus.service.system.impl;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.constants.NameConstant;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.ServicePort;
import com.middleware.caas.common.model.URLInfo;
import com.middleware.zeus.integration.cluster.MysqlReplicateWrapper;
import com.middleware.zeus.integration.cluster.bean.MysqlReplicateCR;
import com.middleware.zeus.integration.platform.PlatformClient;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.service.system.PlatformService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    @Value("${zeus.namespace:zeus}")
    private String zeusNamespace;

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
        Boolean isSwitched = args.getBoolean("isSwitched");
        if (isSwitched != null && isSwitched){
            res.put("isSwitched",true);
        } else {
            res.put("isSwitched",false);
        }

        if (isSource == null || !isSource){
            res.put("isSource",false);
            return res;
        }
        res.put("isSource", isSource);
        JSONObject chief = args.getJSONObject("chief");
        res.put("chief", chief);
        JSONObject relation = args.getJSONObject("relation");
        res.put("relation", relation);

        // TODO 获取主备平台健康状态

        return res;
    }

    @Override
    public void saveRelationAddr(URLInfo urlInfo, String relationName) {
        JSONObject values = helmChartService.getZeusMysqlInstallValues();
        JSONObject newValues = JSONObject.parseObject(values.toJSONString());
        JSONObject relation = newValues.getJSONObject("args").getJSONObject("relation");
        if (relation == null) {
            relation = new JSONObject();
        }
        relation.put("protocol", urlInfo.getProtocol());
        relation.put("host", urlInfo.getHost());
        relation.put("port", urlInfo.getPort());
        relation.put("name", relationName);
        newValues.getJSONObject("args").put("relation",relation);
        helmChartService.upgradeZeusMysql(values,newValues);
    }

    @Override
    public void saveChiefAddr(URLInfo urlInfo, String chiefName) {
        JSONObject values = helmChartService.getZeusMysqlInstallValues();
        JSONObject newValues = JSONObject.parseObject(values.toJSONString());
        JSONObject chief = newValues.getJSONObject("args").getJSONObject("chief");
        if (chief == null){
            chief = new JSONObject();
        }
        chief.put("chiefProtocol", urlInfo.getProtocol());
        chief.put("chiefHost", urlInfo.getHost());
        chief.put("chiefPort", urlInfo.getPort());
        chief.put("chiefName", chiefName);
        newValues.getJSONObject("args").put("chief",chief);
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
            try{
                JSONObject res = platformClient.switchPlatform(request.getHeader("userToken"));
                if (res != null && res.getJSONObject("data").getBoolean("success")){
                    log.info("切换成功，res = {}",res);
                    newValues.put("type","slave-slave");
                    newValues.getJSONObject("args").put("isSource",false);
                    newValues.getJSONObject("args").put("isSwitched",true);
                    helmChartService.upgradeZeusMysql(values,newValues);
                }
            }catch (Exception e){
                log.error("切换失败{}",e.getMessage());
            }
        }else{
            MysqlReplicateCR mr = mysqlReplicateWrapper.getMysqlReplicate(zeusNamespace, NameConstant.ZEUS_MYSQL_REPLICATE);
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
