package com.middleware.zeus.skyview.v2.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.base.CaasResult;
import com.middleware.caas.common.model.QuotaBase;
import com.middleware.caas.common.model.ResourceQuotaDo;
import com.middleware.caas.common.model.StorageQuota;
import com.middleware.caas.common.model.middleware.Namespace;
import com.middleware.caas.common.model.user.OrganizationDto;
import com.middleware.caas.common.model.user.UserDto;
import com.middleware.zeus.service.user.RoleService;
import com.middleware.zeus.skyview.v2.client.V2OrganServiceClient;
import com.middleware.zeus.skyview.v2.service.V2OrganService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2023/3/23 8:19 下午
 */
@Service
@Slf4j
public class V2OrganServiceImpl implements V2OrganService {

    @Autowired
    private V2OrganServiceClient v2OrganServiceClient;
    @Autowired
    private RoleService roleService;


    @Override
    public List<OrganizationDto> list() {
        CaasResult<JSONArray> res = v2OrganServiceClient.list(null);
        return res.getData().stream().map(user -> convertOrgan(JSONObject.parseObject(JSONObject.toJSONString(user))))
                .collect(Collectors.toList());
    }

    @Override
    public OrganizationDto get(String organId) {
        CaasResult<JSONObject> res = v2OrganServiceClient.get(organId, true, true);
        return convertOrgan(res.getData());
    }

    @Override
    public List<Namespace> nsList(String organId) {
        CaasResult<JSONObject> res = v2OrganServiceClient.get(organId, false, false);
        if (!res.getData().containsKey("namespaceList")) {
            return new ArrayList<>();
        }
        return res.getData().getJSONArray("namespaceList").stream()
            .map(ns -> convertNs(JSONObject.parseObject(JSONObject.toJSONString(ns)))).collect(Collectors.toList());
    }


    @Override
    public List<ResourceQuotaDo> quotas(String organId) {
        CaasResult<JSONObject> res = v2OrganServiceClient.quotas(organId, true);
        JSONObject dataCenter = res.getData().getJSONObject("list");
        List<ResourceQuotaDo> resourceQuotaDoList = new ArrayList<>();
        for (String key : dataCenter.keySet()){
            JSONArray quotaList = dataCenter.getJSONArray(key);
            for (int i = 0; i < quotaList.size(); ++i){
                ResourceQuotaDo resourceQuotaDo = convertQuota(quotaList.getJSONObject(i));
                resourceQuotaDoList.add(resourceQuotaDo);
            }

        }
        return resourceQuotaDoList;
    }

    @Override
    public List<UserDto> userList(String organId) {
        CaasResult<JSONArray> res = v2OrganServiceClient.userList(organId);
        List<UserDto> userDtoList = res.getData().stream().map(user -> convertUser(JSONObject.parseObject(JSONObject.toJSONString(user))))
                .collect(Collectors.toList());

        OrganizationDto organizationDto = get(organId);
        if (!CollectionUtils.isEmpty(organizationDto.getUserDtoList())){
            for (UserDto userDto : userDtoList){
                boolean flag =  organizationDto.getUserDtoList().stream().anyMatch(user -> user.getUserName().equals(userDto.getUserName()));
                if (flag){
                    userDto.setRoleId(roleService.getOrganManagerRoleId());
                }
            }
        }
        return res.getData().stream().map(user -> convertUser(JSONObject.parseObject(JSONObject.toJSONString(user))))
                .collect(Collectors.toList());
    }


    public OrganizationDto convertOrgan(JSONObject organ){
        OrganizationDto organDto = new OrganizationDto();
        organDto.setOrganId(organ.getString("tenantId"));
        organDto.setName(organ.getString("aliasName"));
        organDto.setProjectCount(organ.getInteger("projectNum"));

        if (organ.containsKey("tmUserList")){
            List<UserDto> userDtoList = new ArrayList<>();
            JSONArray tmUserList = organ.getJSONArray("tmUserList");
            for (int i = 0; i < tmUserList.size(); ++i){
                JSONObject user = tmUserList.getJSONObject(i);
                UserDto userDto = convertUser(user);
                userDtoList.add(userDto);
            }
            organDto.setUserDtoList(userDtoList);
        }
        // 获取组织下用户数量
        if (organ.containsKey("memberNum")){
            organDto.setUserCount(organ.getInteger("memberNum"));
        }
        return organDto;
    }

    public ResourceQuotaDo convertQuota(JSONObject quota){
        ResourceQuotaDo quotaDo = new ResourceQuotaDo();
        quotaDo.setClusterId(quota.getString("clusterId"));
        quotaDo.setClusterNickName(quota.getString("clusterAliasName"));

        // todo 处理资源单位

        QuotaBase cpu = new QuotaBase();
        cpu.setRequest(quota.getDouble("cpuQuota"));
        cpu.setUsed(quota.getDouble("usedCpu"));
        quotaDo.setCpu(cpu);

        QuotaBase memory = new QuotaBase();
        memory.setRequest(quota.getDouble("memoryQuota"));
        memory.setUsed(quota.getDouble("usedMemory"));

        List<StorageQuota> storageQuotaList = new ArrayList<>();
        JSONArray storageQuotaArray = quota.getJSONArray("storageQuota");
        for (int i = 0; i < storageQuotaArray.size(); ++i){
            JSONObject object = storageQuotaArray.getJSONObject(i);
            if (object.containsKey("allocatedStorage") && object.getDouble("allocatedStorage") != 0){
                StorageQuota storageQuota = new StorageQuota();
                storageQuota.setName(object.getString("name"));
                QuotaBase storage = new QuotaBase();
                storage.setRequest(object.getDouble("storageQuota"));
                storage.setUsed(object.getDouble("allocatedStorage"));

                storageQuota.setStorage(storage);
                storageQuotaList.add(storageQuota);
            }
        }
        quotaDo.setStorageList(storageQuotaList);

        return quotaDo;
    }

    public UserDto convertUser(JSONObject user){
        UserDto userDto = new UserDto();
        userDto.setUserName(user.getString("username"));
        userDto.setAliasName(user.getString("realName"));
        userDto.setEmail(user.getString("email"));
        userDto.setPhone(user.getString("phone"));
        userDto.setIsAdmin(user.getBoolean("admin"));
        return userDto;
    }

    public Namespace convertNs(JSONObject ns){
        Namespace namespace = new Namespace();
        namespace.setName(ns.getString("name"));
        namespace.setAliasName(ns.getString("aliasName"));
        namespace.setClusterId(ns.getString("clusterId"));
        namespace.setClusterAliasName(ns.getString("clusterAliasName"));
        namespace.setOrganId(ns.getString("tenantId"));
        namespace.setProjectId(ns.getString("projectId"));
        namespace.setProjectName(ns.getString("projectName"));
        return namespace;
    }

}
