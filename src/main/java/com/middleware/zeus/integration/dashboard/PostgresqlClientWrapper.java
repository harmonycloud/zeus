package com.middleware.zeus.integration.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2023/4/27 8:04 下午
 */
@Component
@Slf4j
public class PostgresqlClientWrapper {

    @Autowired
    private PostgresqlClient postgresqlClient;


    public void manualSwitch(String svcName, Integer port, String candidate){
        postgresqlClient.manualSwitch(svcName, port, candidate);
    }

    public JSONArray cluster(String svcName, Integer port){
        JSONObject res = postgresqlClient.patroniCluster(svcName, port);
        if (res.containsKey("data")){
            try {
                return JSONObject.parseObject(res.getString("data")).getJSONArray("members");
            } catch (Exception e){
                log.error("查询postgresql patroni集群信息异常,{}", res.getString("data"));
            }
        }
        throw new BusinessException(ErrorMessage.NOT_EXIST_OR_NOT_RUNNING);
    }

}
