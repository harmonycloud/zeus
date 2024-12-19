package com.middleware.zeus.integration.dashboard;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONObject;

import lombok.extern.slf4j.Slf4j;


/**
 * @author xutianhong
 * @Date 2024/12/18 1:46 PM
 */
@Component
@Slf4j
public class MongodbClientWrapper {

    @Autowired
    private MongodbClient mongodbClient;


    public String getOrgId(){
        JSONObject res = mongodbClient.getOrgId();
        if (res.containsKey("data")){
            try {
                return res.getJSONObject("data").getString("id");
            } catch (Exception e){
                log.error("查询mongodb orgId 失败");
            }
        }
        return null;
    }

}
