package com.middleware.zeus.skyview.v2.service.impl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.base.CaasResult;
import com.middleware.caas.common.enums.DateType;
import com.middleware.caas.common.model.ResourceQuotaDo;
import com.middleware.caas.common.model.user.UserDto;
import com.middleware.caas.common.model.user.UserRole;
import com.middleware.tool.date.DateUtils;
import com.middleware.zeus.skyview.v2.client.V2UserServiceClient;
import com.middleware.zeus.skyview.v2.service.V2UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
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

    @Override
    public UserDto get(String username) {
        CaasResult<JSONArray> res = v2UserServiceClient.getUser(null, username);
        JSONObject user = res.getData().getJSONObject(0);
        return convertUser(user);
    }

    @Override
    public List<UserDto> list() {
        CaasResult<JSONArray> res = v2UserServiceClient.getUser(null, null);
        return res.getData().stream().map(user -> convertUser(JSONObject.parseObject(JSONObject.toJSONString(user))))
            .collect(Collectors.toList());
    }



    public UserDto convertUser(JSONObject user){
        UserDto userDto = new UserDto();
        userDto.setUserName(user.getString("username"));
        userDto.setAliasName(user.getString("realName"));
        userDto.setEmail(user.getString("email"));
        userDto.setPhone(user.getString("phone"));
        userDto.setCreateTime(DateUtils.parseDate(user.getString("createTime"), DateType.YYYY_MM_DD_T_HH_MM_SS.getValue()));

        if (user.containsKey("otherProjects")){
            List<UserRole> userRoleList = new ArrayList<>();
            JSONArray projectList = user.getJSONArray("otherProjects");
            for (JSONObject project : projectList.toJavaList(JSONObject.class)){
                // todo 需处理租户管理员下包含所有项目的问题
                UserRole userRole = new UserRole();
                if (StringUtils.isEmpty(project.getString("roleId"))){
                    continue;
                }
                userRole.setOrganId(project.getString("tenantId"));
                userRole.setProjectId(project.getString("projectId"));
                userRole.setProjectName(project.getString("projectAliasName"));
                userRole.setRoleId(project.getInteger("roleId"));
                userRole.setUserName(userDto.getUserName());
                // todo 需转换为中间件平台角色名称
                userRole.setRoleName(project.getString("roleNickName"));
                userRoleList.add(userRole);
            }
            userDto.setUserRoleList(userRoleList);
        }
        return userDto;
    }
}
