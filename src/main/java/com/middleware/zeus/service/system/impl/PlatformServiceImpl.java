package com.middleware.zeus.service.system.impl;

import com.alibaba.fastjson.JSONObject;
import com.dtflys.forest.exceptions.ForestNetworkException;
import com.dtflys.forest.exceptions.ForestRuntimeException;
import com.middleware.caas.common.constants.NameConstant;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.DisasterRecoveryDto;
import com.middleware.caas.common.model.DisasterRecoveryInfo;
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
import java.util.Date;

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
    public DisasterRecoveryDto queryAccessInfo() {
        DisasterRecoveryDto res = new DisasterRecoveryDto();
        JSONObject values = helmChartService.getZeusMysqlInstallValues();

        // 是否主平台
        res.setIsMaster("master-slave".equals(values.getString("type")));

        // 链接地址信息
        JSONObject args = values.getJSONObject("args");
        JSONObject _local = args.getJSONObject("local");
        JSONObject _remote = args.getJSONObject("relation");
        res.setLocal(convertParam(_local)).setRelation(convertParam(_remote));

        // 上次切换时间
        res.setLastSwitchTime(values.getDate("lastSwitchTime"));

        // TODO 平台健康状态
        return res;
    }

    private DisasterRecoveryInfo convertParam(JSONObject info) {
        if (info == null) {
            return null;
        }
        return new DisasterRecoveryInfo().setHost(info.getString("host")).
                setProtocol(info.getString("protocol")).
                setPort(info.getInteger("port")).
                setName(info.getString("name"));
    }

    @Override
    public void switchPlatform(HttpServletRequest request) throws IOException {
        JSONObject values = helmChartService.getZeusMysqlInstallValues();
        JSONObject newValues = JSONObject.parseObject(values.toJSONString());
        Boolean isMaster = "master-slave".equals(values.getString("type"));
        if (isMaster) {
            try {
                JSONObject res = platformClient.switchPlatform(request.getHeader("userToken"));
                log.info("切换结果:{}",res);
                if (res != null && res.getBoolean("success")) {
                    log.info("切换成功，res = {}", res);
                    newValues.put("type", "slave-slave");
                    newValues.put("lastSwitchTime",new Date());
                    newValues.getJSONObject("args").put("disasterRecoverySwitched",true);
                    helmChartService.upgradeZeusMysql(values, newValues);
                } else {
                    log.error("切换失败,res={}",res);
                    throw new BusinessException(ErrorMessage.REMOTE_SWITCH_FAILED);
                }
            } catch (ForestNetworkException e) {
                log.error("切换失败", e);
                throw new BusinessException(ErrorMessage.CONNECT_REMOTE_HOST_FAILED);
            }
        } else {
            log.info("备平台接收切换请求，开始灾备切换");
            try{
                MysqlReplicateCR mr =
                        mysqlReplicateWrapper.getMysqlReplicate(zeusNamespace, NameConstant.ZEUS_MYSQL_REPLICATE);
                mr.getSpec().setEnable(false);
                mysqlReplicateWrapper.updateMysqlReplicate(mr);
                JSONObject args = newValues.getJSONObject("args");
                newValues.put("type", "master-slave");
                newValues.put("lastSwitchTime",new Date());
                newValues.getJSONObject("args").put("disasterRecoverySwitched",true);
                helmChartService.upgradeZeusMysql(values, newValues);
            }catch (Exception e){
                log.error("切换失败",e);
                throw new BusinessException(ErrorMessage.REMOTE_SWITCH_FAILED);
            }
        }
    }

    @Override
    public void saveAddr(DisasterRecoveryInfo info, String name) {
        JSONObject values = helmChartService.getZeusMysqlInstallValues();
        JSONObject newValues = JSONObject.parseObject(values.toJSONString());
        JSONObject addrInfo = new JSONObject();
        addrInfo.put("protocol", info.getProtocol());
        addrInfo.put("host", info.getHost());
        addrInfo.put("port", info.getPort());
        addrInfo.put("name", name);
        newValues.getJSONObject("args").put(info.getIsRelation() ? "relation" : "local", addrInfo);
        helmChartService.upgradeZeusMysql(values, newValues);
    }

}
