package com.middleware.zeus.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.model.user.ResourceMenuDto;
import com.middleware.zeus.bean.user.BeanResourceMenuRole;
import com.middleware.zeus.dao.user.BeanResourceMenuRoleMapper;
import com.middleware.zeus.service.user.ResourceMenuRoleService;
import com.middleware.zeus.service.user.ResourceMenuService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/7/29 9:38 上午
 */
@Service
@Slf4j
public class ResourceMenuRoleServiceImpl implements ResourceMenuRoleService {

    @Value("${system.user.admin.role-id:1}")
    private String adminRoleId;
    @Value("${system.user.role.resource-menu:5,8,9,10,12,14,15,16}")
    private String opsIds;

    @Autowired
    private BeanResourceMenuRoleMapper beanResourceMenuRoleMapper;
    @Autowired
    private ResourceMenuService resourceMenuService;

    @Override
    public void init(Integer roleId) {
        List<ResourceMenuDto> menuDtoList = resourceMenuService.list();
        menuDtoList.forEach(menu -> add(roleId, menu.getId(), menu.getId() == 3 || menu.getId() == 4));
    }

    @Override
    public List<BeanResourceMenuRole> listAdminMenu() {
        return list(adminRoleId);
    }

    @Override
    public List<BeanResourceMenuRole> list(String roleId) {
        QueryWrapper<BeanResourceMenuRole> rmRoleWrapper =
            new QueryWrapper<BeanResourceMenuRole>().eq("available", 1);
        if (StringUtils.isNotEmpty(roleId)){
            rmRoleWrapper.eq("role_id", roleId);
        }
        return beanResourceMenuRoleMapper.selectList(rmRoleWrapper);
    }

    @Override
    public void add(Integer roleId, Integer resourceMenuId, Boolean available) {
        BeanResourceMenuRole beanResourceMenuRole = new BeanResourceMenuRole();
        beanResourceMenuRole.setRoleId(roleId);
        beanResourceMenuRole.setResourceMenuId(resourceMenuId);
        beanResourceMenuRole.setAvailable(available);
        beanResourceMenuRoleMapper.insert(beanResourceMenuRole);
    }

    @Override
    public void delete(Integer roleId) {
        QueryWrapper<BeanResourceMenuRole> wrapper = new QueryWrapper<BeanResourceMenuRole>().eq("role_id", roleId);
        beanResourceMenuRoleMapper.delete(wrapper);
    }

    @Override
    public void update(Integer roleId, Integer resourceMenuId, Boolean available) {
        QueryWrapper<BeanResourceMenuRole> wrapper =
            new QueryWrapper<BeanResourceMenuRole>().eq("role_id", roleId).eq("resource_menu_id", resourceMenuId);
        BeanResourceMenuRole beanResourceMenuRole = new BeanResourceMenuRole();
        beanResourceMenuRole.setAvailable(available);
        beanResourceMenuRoleMapper.update(beanResourceMenuRole, wrapper);
    }

    @Override
    public void updateOpsMenu(Integer roleId, boolean ops) {
        List<String> optIdList = Arrays.asList(opsIds.split(","));
        optIdList.forEach(optId -> this.update(roleId, Integer.parseInt(optId), ops));
    }
}
