package com.middleware.zeus.skyview.v2.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.base.CaasResult;
import com.middleware.caas.common.enums.CaasRole;
import com.middleware.caas.common.enums.DateType;
import com.middleware.caas.common.model.ResourceQuotaDo;
import com.middleware.caas.common.model.user.ProjectDto;
import com.middleware.caas.common.model.user.UserDto;
import com.middleware.caas.common.model.user.UserRole;
import com.middleware.tool.date.DateUtils;
import com.middleware.zeus.service.user.RoleService;
import com.middleware.zeus.skyview.v2.client.V2ProjectServiceClient;
import com.middleware.zeus.skyview.v2.client.V2UserServiceClient;
import com.middleware.zeus.skyview.v2.service.V2UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2023/3/21 7:06 下午
 */
@Service
@Slf4j
public class V2UserServiceImpl implements V2UserService {

    @Autowired
    private V2UserServiceClient v2UserServiceClient;
    @Autowired
    private RoleService roleService;
    @Autowired
    private V2ProjectServiceImpl v2ProjectService;

    @Override
    public UserDto get(String username) {
        CaasResult<JSONArray> res = v2UserServiceClient.getUser(username);
        JSONObject user = res.getData().getJSONObject(0);
        return convertUser(user);
    }

    @Override
    public List<UserDto> list() {
        CaasResult<JSONArray> res = v2UserServiceClient.listUser(null);
        return res.getData().stream().map(user -> convertUser(JSONObject.parseObject(JSONObject.toJSONString(user))))
            .collect(Collectors.toList());
    }



    public UserDto convertUser(JSONObject user){
        UserDto userDto = new UserDto();
        userDto.setUserName(user.getString("username"));
        userDto.setAliasName(user.getString("realName"));
        userDto.setEmail(user.getString("email"));
        userDto.setPhone(user.getString("phone"));

        try {
            String createTime = user.getString("createTime");
            Date date = new SimpleDateFormat(DateType.EEE_MMM_DD_HH_MM_SS_ZZZ_YYYY.getValue(), java.util.Locale.ENGLISH).parse(createTime);
            userDto.setCreateTime(date);
        } catch (Exception e){
            log.error("转换失败", e);
        }

        userDto.setIsAdmin(user.getBoolean("admin"));

        if (user.containsKey("otherProjects")){
            List<UserRole> userRoleList = new ArrayList<>();
            JSONArray projectList = user.getJSONArray("otherProjects");

            Map<String, String> organMap = new HashMap<>();
            for (JSONObject project : projectList.toJavaList(JSONObject.class)){
                UserRole userRole = new UserRole();
                if (StringUtils.isEmpty(project.getString("roleId"))){
                    continue;
                }
                String organId = project.getString("tenantId");

                userRole.setOrganId(organId);
                userRole.setOrganName(project.getString("tenantName"));
                userRole.setProjectId(project.getString("projectId"));
                userRole.setProjectName(project.getString("projectAliasName"));

                userRole.setUserName(userDto.getUserName());
                // 转换为中间件平台角色名称
                CaasRole caasRole = CaasRole.findByName(project.getString("roleName"));
                if (caasRole != null){
                    if (caasRole.getId() == 5){
                        userDto.setRoleId(roleService.getOrganManagerRoleId().getId());
                        organMap.put(organId, userRole.getOrganName());
                    }else {
                        userDto.setRoleId(caasRole.getId());
                    }
                    userRole.setRoleName(caasRole.getRoleName());
                }
                userRole.setRoleId(project.getInteger("roleId"));
                userRole.setRoleName(project.getString("roleNickName"));
                userRoleList.add(userRole);
            }
            // 处理租户管理员应包含所有项目的项目管理员
            userRoleList.addAll(solveOrganManager(userRoleList, organMap));

            userDto.setUserRoleList(userRoleList);
        }
        return userDto;
    }

    public List<UserRole> solveOrganManager(List<UserRole> userRoleList, Map<String, String> organMap){
        // 过滤掉额外的已是租户管理员的租户下的项目信息
        userRoleList = userRoleList.stream()
            .filter(userRole -> StringUtils.isNoneEmpty(userRole.getOrganId(), userRole.getProjectId())
                && organMap.keySet().stream().anyMatch(organId -> organId.equals(userRole.getOrganId())))
            .collect(Collectors.toList());

        // 查询租户下的所有项目
        for (String organId : organMap.keySet()){
            List<ProjectDto> projectDtoList = v2ProjectService.list(organId);
            for (ProjectDto projectDto : projectDtoList){
                UserRole userRole = new UserRole();
                userRole.setProjectId(projectDto.getProjectId());
                userRole.setProjectName(projectDto.getName());
                userRole.setRoleId(CaasRole.PM.getId());
                userRole.setRoleName(CaasRole.PM.getRoleName());
                userRole.setOrganId(organId);
                userRole.setOrganName(organMap.get(organId));
                userRoleList.add(userRole);
            }
        }
        return userRoleList;
    }
}
