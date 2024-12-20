package com.middleware.zeus.integration.dashboard;

import com.alibaba.fastjson.JSONArray;
import com.middleware.zeus.common.enums.Protocol;
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


    public String getOrgId(String publicKey, String privateKey){
        String protocol = Protocol.HTTP.getValue().toLowerCase();
        String path = "mongodb-enterprise-operator-om-svc.middleware-operator";
        String port = "8080";
        JSONObject res = mongodbClient.getOrgId(protocol, path, port, publicKey, privateKey);
        if (res.containsKey("data")){
            try {
                JSONArray data = res.getJSONArray("data");
                for (int i = 0; i < data.size(); i++) {
                    JSONObject obj = data.getJSONObject(i);
                    if (obj.getString("name").equals("mongodb-enterprise-operator-om-db")) {
                        return obj.getString("id");
                    }
                }
            } catch (Exception e){
                log.error("查询mongodb orgId 失败");
            }
        }
        return null;
    }

    public void deleteProject(String publicKey, String privateKey, String name){
        String protocol = Protocol.HTTP.getValue().toLowerCase();
        String path = "mongodb-enterprise-operator-om-svc.middleware-operator";
        String port = "8080";
        mongodbClient.deleteProject(protocol, path, port, publicKey, privateKey, name);
    }

}
