package com.middleware.zeus.skyview.v2.service.impl;

import static com.middleware.caas.common.constants.NameConstant.CPU;
import static com.middleware.caas.common.constants.NameConstant.MEMORY;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.middleware.caas.common.enums.CaasRole;
import com.middleware.caas.common.model.user.UserDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.base.CaasResult;
import com.middleware.caas.common.model.QuotaBase;
import com.middleware.caas.common.model.ResourceQuotaDo;
import com.middleware.caas.common.model.StorageQuota;
import com.middleware.caas.common.model.middleware.Namespace;
import com.middleware.caas.common.model.user.ProjectDto;
import com.middleware.zeus.skyview.v2.client.V2ProjectServiceClient;
import com.middleware.zeus.skyview.v2.service.V2ProjectService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2023/3/24 4:07 下午
 */
@Slf4j
@Service
public class V2ProjectServiceImpl implements V2ProjectService {

    @Autowired
    private V2ProjectServiceClient v2ProjectServiceClient;

    @Override
    public List<ProjectDto> list(String organId) {
        CaasResult<JSONArray> res = v2ProjectServiceClient.list(organId, true, true, true);
        return res.getData().stream().map(project -> convertProject(JSONObject.parseObject(JSONObject.toJSONString(project))))
            .collect(Collectors.toList());
    }

    @Override
    public List<ProjectDto> switchTenants(String organId) {
        CaasResult<JSONObject> res = v2ProjectServiceClient.switchTenants(organId);
        if (res.getData().containsKey("projectList")) {
            JSONArray projectArray = res.getData().getJSONArray("projectList");
            return projectArray.stream()
                .map(project -> convertProject(JSONObject.parseObject(JSONObject.toJSONString(project))))
                .collect(Collectors.toList());
        }
        return null;
    }

    @Override
    public List<Namespace> nsList(String organId, String projectId, Boolean withQuota) {
        CaasResult<JSONArray> res = v2ProjectServiceClient.nsList(organId, projectId, withQuota, true);

        return res.getData().stream().map(ns -> convertNamespace(JSONObject.parseObject(JSONObject.toJSONString(ns))))
            .collect(Collectors.toList());
    }

    @Override
    public ProjectDto get(String organId, String projectId, Boolean includeNsCount, Boolean includeQuota) {
        CaasResult<JSONObject> res = v2ProjectServiceClient.get(organId, projectId, includeNsCount, includeQuota, false);
        return convertProject(res.getData());
    }

    public ProjectDto convertProject(JSONObject project) {
        ProjectDto projectDto = new ProjectDto();
        projectDto.setProjectId(project.getString("projectId"));
        projectDto.setName(project.getString("aliasName"));
        projectDto.setOrganId(project.getString("tenantId"));
        projectDto.setMemberCount(project.getInteger("userNum"));

        JSONArray namespaceArray = project.getJSONArray("namespaceList");
        if (CollectionUtils.isEmpty(namespaceArray)){
            projectDto.setNamespaceCount(namespaceArray.size());
        }


        if (project.containsKey("userDataList") && !CollectionUtils.isEmpty(project.getJSONArray("userDataList"))) {
            List<UserDto> userDtoList = new ArrayList<>();
            JSONArray userArray = project.getJSONArray("userDataList");
            for (int i = 0; i < userArray.size(); ++i){
                JSONObject user = userArray.getJSONObject(i);
                UserDto userDto = new UserDto();
                userDto.setUserName(user.getString("username"));
                userDto.setAliasName(user.getString("nickName"));

                //转换角色
                JSONObject role = user.getJSONObject("role");
                CaasRole caasRole = CaasRole.findByName(role.getString("name"));
                if (caasRole != null){
                    userDto.setRoleId(caasRole.getId());
                    userDto.setRoleName(caasRole.getRoleName());
                }
                userDtoList.add(userDto);
            }
            projectDto.setUserDtoList(userDtoList);
        }

        return projectDto;
    }

    public Namespace convertNamespace(JSONObject ns) {
        Namespace namespace = new Namespace();
        namespace.setName(ns.getString("name"));
        namespace.setAliasName(ns.getString("aliasName"));
        namespace.setClusterId(ns.getString("clusterId"));
        namespace.setClusterAliasName(ns.getString("clusterAliasName"));
        namespace.setOrganId(ns.getString("tenantId"));
        namespace.setProjectId(ns.getString("projectId"));
        namespace.setProjectName(ns.getString("projectName"));

        ResourceQuotaDo resourceQuotaDo = new ResourceQuotaDo();
        if (ns.containsKey(CPU) && !CollectionUtils.isEmpty(ns.getJSONArray(CPU))) {
            JSONArray cpuArray = ns.getJSONArray(CPU);
            QuotaBase cpu = new QuotaBase();
            cpu.setRequest(cpuArray.getDouble(0));
            cpu.setUsed(cpuArray.getDouble(1));
        }

        if (ns.containsKey(MEMORY) && !CollectionUtils.isEmpty(ns.getJSONArray(MEMORY))) {
            JSONArray memoryArray = ns.getJSONArray(MEMORY);
            QuotaBase memory = new QuotaBase();
            memory.setUsed(memoryArray.getDouble(1));
            memory.setRequest(memoryArray.getDouble(0));
        }

        if (ns.containsKey("storageclasses")) {
            List<StorageQuota> storageQuotaList = new ArrayList<>();
            JSONObject storageObject = ns.getJSONObject("storageclasses");
            for (String key : storageObject.keySet()) {
                if (storageObject.containsKey(key) && !CollectionUtils.isEmpty(storageObject.getJSONArray(key))) {
                    JSONArray storageArray = storageObject.getJSONArray(key);
                    StorageQuota storageQuota = new StorageQuota();
                    storageQuota.setName(key);
                    QuotaBase storage = new QuotaBase();
                    storage.setRequest(storageArray.getDouble(0));
                    storage.setUsed(storageArray.getDouble(1));
                    storageQuotaList.add(storageQuota);
                }
            }
            resourceQuotaDo.setStorageList(storageQuotaList);
        }

        namespace.setQuotas(resourceQuotaDo);

        return namespace;
    }
}
