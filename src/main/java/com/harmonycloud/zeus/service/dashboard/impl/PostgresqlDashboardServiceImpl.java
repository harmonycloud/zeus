package com.harmonycloud.zeus.service.dashboard.impl;

import com.alibaba.fastjson.JSONArray;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.dashboard.DatabaseDto;
import com.harmonycloud.caas.common.model.dashboard.MiddlewareUserAuthority;
import com.harmonycloud.caas.common.model.dashboard.MiddlewareUserDto;
import com.harmonycloud.zeus.util.PostgresqlAuthorityUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.zeus.integration.dashboard.PostgresqlClient;
import com.harmonycloud.zeus.service.dashboard.PostgresqlDashboardService;

import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2022/10/11 2:50 下午
 */
@Service
@Slf4j
public class PostgresqlDashboardServiceImpl implements PostgresqlDashboardService {

    @Autowired
    private PostgresqlClient postgresqlClient;

    @Override
    public List<DatabaseDto> listDatabases(String clusterId, String namespace, String middlewareName) {
        JSONObject listDatabases = postgresqlClient.listDatabases(middlewareName, "31503");
        List<Map<String, String>> databaseList = convertColumn(listDatabases);
        return databaseList.stream().map(database -> {
            DatabaseDto databaseDto = new DatabaseDto();
            databaseDto.setOid(database.get("oid"));
            databaseDto.setDatabaseName(database.get("datname"));
            databaseDto.setEncoding(database.get("encoding"));
            databaseDto.setCollate(database.get("datcollate"));
            databaseDto.setOwner(database.get("datdba"));
            return databaseDto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<MiddlewareUserDto> listUser(String clusterId, String namespace, String middlewareName, String keyword) {
        JSONObject listUsers = postgresqlClient.listUsers(middlewareName, "31503");
        List<Map<String, String>> res = convertColumn(listUsers);

        return res.stream().map(map -> {
            MiddlewareUserDto middlewareUserDto = new MiddlewareUserDto();
            middlewareUserDto.setId(map.get("oid"));
            middlewareUserDto.setUsername(map.get("rolname"));
            middlewareUserDto.setUsable(Boolean.parseBoolean(map.get("rolcanlogin")));
            return middlewareUserDto;
        }).collect(Collectors.toList());
    }

    @Override
    public void addUser(String clusterId, String namespace, String middlewareName, MiddlewareUserDto middlewareUserDto) {
        JSONObject addUser = postgresqlClient.addUser(middlewareName, "31503", middlewareUserDto.getUsername(), middlewareUserDto.getPassword());
        JSONObject err = addUser.getJSONObject("err");
        if (err != null){
            throw new BusinessException(ErrorMessage.CREATE_USER_FAILED, err.getString("Message"));
        }
    }

    @Override
    public void dropUser(String clusterId, String namespace, String middlewareName, String username) {
        JSONObject dropUser = postgresqlClient.dropUser(middlewareName, "31503", username);
        JSONObject err = dropUser.getJSONObject("err");
        if (err != null){
            throw new BusinessException(ErrorMessage.DROP_USER_FAILED, err.getString("Message"));
        }
    }

    @Override
    public List<MiddlewareUserAuthority> userAuthority(String clusterId, String namespace, String middlewareName, String username) {
        JSONObject listDatabases = postgresqlClient.listDatabases(middlewareName, "31503");
        List<Map<String, String>> databaseList = convertColumn(listDatabases);

        List<MiddlewareUserAuthority> userAuthorityList = new ArrayList<>();
        databaseList.forEach(database -> {
            String datAcl = database.get("datacl");
            if (StringUtils.isEmpty(datAcl) || !datAcl.contains(username + "=")){
                // todo 查询owner
                return;
            }
            MiddlewareUserAuthority userAuthority = new MiddlewareUserAuthority();
            userAuthority.setDatabase(database.get("datname"));
            userAuthority.setAuthority(PostgresqlAuthorityUtil.checkAuthority(datAcl, username));
            userAuthorityList.add(userAuthority);
        });
        return userAuthorityList;
    }

    public List<Map<String, String>> convertColumn(JSONObject jsonObject){
        JSONArray data = jsonObject.getJSONArray("data");
        JSONArray column = jsonObject.getJSONArray("column");

        List<Map<String, String>> res = new ArrayList<>();
        List<String> columnList = column.toJavaList(String.class);
        for (int i = 0; i < data.size(); ++i){
            List<String> dataList = data.getJSONArray(i).toJavaList(String.class);
            Map<String, String> map = new HashMap<>();
            for (int j = 0; j < dataList.size(); ++j){
                map.put(columnList.get(j), dataList.get(j));
            }
            res.add(map);
        }
        return res;
    }
}
