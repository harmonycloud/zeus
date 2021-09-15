package com.harmonycloud.zeus.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.zeus.bean.user.BeanResourceMenuRole;
import com.harmonycloud.zeus.dao.user.BeanResourceMenuRoleMapper;
import com.harmonycloud.zeus.service.user.ResourceMenuRoleService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/7/29 9:38 上午
 */
@Service
@Slf4j
public class ResourceMenuRoleServiceImpl implements ResourceMenuRoleService {

    @Autowired
    private BeanResourceMenuRoleMapper beanResourceMenuRoleMapper;
    
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
}
