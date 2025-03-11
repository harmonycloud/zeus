package com.middleware.zeus.integration.dashboard;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.common.enums.Protocol;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbOrgDo;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbProjectDo;
import com.middleware.zeus.common.model.middleware.mongodb.MongodbUserDo;


import lombok.extern.slf4j.Slf4j;


/**
 * @author xutianhong
 * @Date 2024/12/18 1:46 PM
 */
@Component
@Slf4j
public class  MongodbClientWrapper {


    @Value("${system.opsManager.path:mongodb-enterprise-operator-om-svc.middleware-operator}")
    private String path;
    @Value("${system.opsManager.port:8080}")
    private String port;

    @Autowired
    private MongodbClient mongodbClient;


    /**
     * 获取组织id
     * @param publicKey 公钥
     * @param privateKey 私钥
     * @return String
     */
    public String getOrgId(String publicKey, String privateKey){
        String protocol = Protocol.HTTP.getValue().toLowerCase();
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

    /**
     * 创建用户
     * @param clusterId 集群id
     * @param mongodbUserDo 用户信息
     */
    public void createUser(String clusterId, MongodbUserDo mongodbUserDo) {
        try {
            mongodbClient.createUser(clusterId, mongodbUserDo);
        } catch (Exception e){
            log.error("创建ops manager用户失败", e);
        }
    }

    /**
     * 删除用户
     * @param clusterId 集群id
     * @param userId 用户id
     */
    public void deleteUser(String clusterId, String publicKey, String privateKey, String userId) {
        try {
            mongodbClient.deleteUser(clusterId, Protocol.HTTP.getValue().toLowerCase(), path, port, publicKey, privateKey, userId);
        } catch (Exception e){
            log.error("删除ops manager用户失败", e);
        }
    }

    /**
     * 创建组织
     * @param clusterId 集群id
     * @param mongodbOrgDo 组织信息
     */
    public void createOrg(String clusterId, MongodbOrgDo mongodbOrgDo) {
        try {
            mongodbClient.createOrg(clusterId, mongodbOrgDo);
        } catch (Exception e){
            log.error("创建ops manager组织失败", e);
        }
    }

    /**
     * 更新组织
     * @param mongodbOrgDo 组织信息
     */
    public void updateOrgan(String clusterId, MongodbOrgDo mongodbOrgDo) {
        try {
            mongodbClient.createOrg(clusterId, mongodbOrgDo);
        } catch (Exception e){
            log.error("创建ops manager组织失败", e);
        }
    }

    /**
     * 删除组织
     * @param publicKey 公钥
     * @param privateKey 私钥
     * @param id 组织id
     */
    public void deleteOrg(String clusterId, String publicKey, String privateKey, String id) {
        try {
            mongodbClient.deleteOrg(clusterId, Protocol.HTTP.getValue().toLowerCase(), path, port, publicKey, privateKey, id);
        } catch (Exception e){
            log.error("删除ops manager组织失败", e);
        }
    }

    /**
     * 查询组织列表
     * @param publicKey 公钥
     * @param privateKey 私钥
     * @return List<MongodbOrgDo>
     */
    public List<MongodbOrgDo> listOrgans(String clusterId, String publicKey, String privateKey) {
        JSONObject res =
            mongodbClient.listOrgans(clusterId, Protocol.HTTP.getValue().toLowerCase(), path, port, publicKey, privateKey);
        if (res.containsKey("data")) {
            try {
                JSONArray data = res.getJSONArray("data");
                return data.toJavaList(MongodbOrgDo.class);
            } catch (Exception e) {
                log.error("查询ops manager组织列表失败");
            }
        }
        return null;
    }

    /**
     * 将用户分配给项目
     * @param clusterId 集群id
     * @param mongodbUserDo 用户信息
     */
    public void allocateUserToOrgan(String clusterId, MongodbUserDo mongodbUserDo) {
        try {
            mongodbClient.allocateUserToOrgan(clusterId, mongodbUserDo);
        } catch (Exception e){
            log.error("分配ops manager用户失败", e);
        }
    }

    /**
     * 创建项目
     * @param clusterId 集群id
     * @param mongodbProjectDo 项目信息
     */
    public void createProject(String clusterId, MongodbProjectDo mongodbProjectDo) {
        // 创建项目
        try {
            mongodbClient.createProject(clusterId, mongodbProjectDo);
        } catch (Exception e){
            log.error("创建ops manager项目失败", e);
        }
    }

    /**
     * 更新项目
     * @param mongodbProjectDo 项目信息
     */
    public void updateProject(MongodbProjectDo mongodbProjectDo) {
        // 创建项目
        try {
            mongodbClient.updateProject(mongodbProjectDo);
        } catch (Exception e) {
            log.error("更新ops manager项目失败", e);
        }
    }

    /**
     * 删除项目
     * @param publicKey 公钥
     * @param privateKey 私钥
     * @param id 项目id
     */
    public void deleteProject(String clusterId, String publicKey, String privateKey, String id) {
        // 创建项目
        try {
            mongodbClient.deleteProject(clusterId, Protocol.HTTP.getValue().toLowerCase(), path, port, publicKey, privateKey, id);
        } catch (Exception e){
            log.error("删除ops manager项目失败", e);
        }
    }

    /**
     * 查询项目列表
     * @param publicKey 公钥
     * @param privateKey 私钥
     * @return List<MongodbProjectDo>
     */
    public List<MongodbProjectDo> listAllProjects(String clusterId, String publicKey, String privateKey) {
        JSONObject res =
            mongodbClient.listAllProjects(clusterId, Protocol.HTTP.getValue().toLowerCase(), path, port, publicKey, privateKey);
        if (res.containsKey("data")) {
            try {
                JSONArray data = res.getJSONArray("data");
                return data.toJavaList(MongodbProjectDo.class);
            } catch (Exception e) {
                log.error("查询ops manager项目列表失败");
            }
        }
        return null;
    }

    /**
     * 将用户分配给项目
     * @param clusterId 集群id
     * @param mongodbUserDo 用户信息
     */
    public void allocateUserToProject(String clusterId, MongodbUserDo mongodbUserDo) {
        try {
            mongodbClient.allocateUserToProject(clusterId, mongodbUserDo);
        } catch (Exception e){
            log.error("分配ops manager用户失败", e);
        }
    }

    /**
     * 获取组织下所有用户
     * @param publicKey 公钥
     * @param privateKey 私钥
     * @param orgId 组织id
     * @param projectId 项目id
     * @return JSONObject
     */
    public List<MongodbUserDo> listOrganUser(String clusterId, String publicKey, String privateKey, String orgId, String projectId) {
        JSONObject res = mongodbClient.listOrganUser(clusterId, publicKey, privateKey, orgId, projectId);
        if (res.containsKey("data")) {
            try {
                JSONArray data = res.getJSONArray("data");
                return data.toJavaList(MongodbUserDo.class);
            } catch (Exception e) {
                log.error("查询ops manager 组织下用户列表失败");
            }
        }
        return null;
    }

    /**
     * 获取项目下所有用户
     * @param publicKey 公钥
     * @param privateKey 私钥
     * @param orgId 组织id
     * @param projectId 项目id
     * @return JSONObject
     */
    public List<MongodbUserDo> listProjectUser(String clusterId, String publicKey, String privateKey, String orgId, String projectId) {
        JSONObject res = mongodbClient.listProjectUser(clusterId, publicKey, privateKey, orgId, projectId);
        if (res.containsKey("data")) {
            try {
                JSONArray data = res.getJSONArray("data");
                return data.toJavaList(MongodbUserDo.class);
            } catch (Exception e) {
                log.error("查询ops manager 项目下用户列表失败");
            }
        }
        return null;
    }


}
