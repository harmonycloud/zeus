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
import org.springframework.util.CollectionUtils;

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

    private static Map<String, List<ProjectDto>> ORGAN_PROJECT_MAP = new HashMap<>();

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
        UserDto userDto = convertUser(user);
        ORGAN_PROJECT_MAP.clear();
        return userDto;
    }

    @Override
    public List<UserDto> list() {
        CaasResult<JSONArray> res = v2UserServiceClient.listUser(null);
        List<UserDto> userDtoList = res.getData().stream().map(user -> convertUser(JSONObject.parseObject(JSONObject.toJSONString(user))))
                .collect(Collectors.toList());
        ORGAN_PROJECT_MAP.clear();
        return userDtoList;
    }



    public UserDto convertUser(JSONObject user){
        UserDto userDto = new UserDto();
        userDto.setUserName(user.getString("username"));
        userDto.setAliasName(user.getString("realName"));
        userDto.setEmail(user.getString("email"));
        userDto.setPhone(user.getString("phone"));

        String createTime = user.getString("createTime");
        userDto.setCreateTime(convertCreateTime(createTime));

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
                        userRole.setRoleId(roleService.getOrganManagerRoleId().getId());
                        organMap.put(organId, userRole.getOrganName());
                    }else {
                        userRole.setRoleId(caasRole.getId());
                    }
                    userRole.setRoleName(caasRole.getRoleName());
                }
                userRoleList.add(userRole);
            }
            // 处理租户管理员应包含所有项目的项目管理员
            if(!CollectionUtils.isEmpty(organMap)){
                userRoleList = solveOrganManager(userRoleList, organMap, userDto.getUserName());
            }

            userDto.setUserRoleList(userRoleList);
        }
        return userDto;
    }

    public List<UserRole> solveOrganManager(List<UserRole> userRoleList, Map<String, String> organMap, String username){
        // 过滤掉额外的已是租户管理员的租户下的项目信息
        userRoleList = userRoleList.stream().filter(
            userRole -> (StringUtils.isEmpty(userRole.getOrganId()) || StringUtils.isEmpty(userRole.getProjectId()))
                || (StringUtils.isNoneEmpty(userRole.getOrganId(), userRole.getProjectId())
                    && organMap.keySet().stream().noneMatch(organId -> organId.equals(userRole.getOrganId()))))
            .collect(Collectors.toList());

        // 查询租户下的所有项目
        for (String organId : organMap.keySet()){
            List<ProjectDto> projectDtoList;
            // 通过缓存数据获取租户下的项目信息
            if (ORGAN_PROJECT_MAP.containsKey(organId)){
                projectDtoList = ORGAN_PROJECT_MAP.get(organId);
            } else {
                projectDtoList = v2ProjectService.list(organId);
                ORGAN_PROJECT_MAP.put(organId, projectDtoList);
            }
            for (ProjectDto projectDto : projectDtoList){
                UserRole userRole = new UserRole();
                userRole.setUserName(username);
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


    public Date convertCreateTime(String createTime) {
        Date date = null;
        try {
            date = new SimpleDateFormat(DateType.EEE_MMM_DD_HH_MM_SS_ZZZ_YYYY.getValue(), java.util.Locale.ENGLISH)
                .parse(createTime);
        } catch (Exception ignored) {
        }
        if (date == null) {
            try {
                date = new SimpleDateFormat(DateType.YYYY_MM_DD_T_HH_MM_SS.getValue()).parse(createTime);
            } catch (Exception ignored) {
            }
        }
        return date;
    }
}
