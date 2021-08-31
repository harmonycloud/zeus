package com.harmonycloud.zeus.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.zeus.bean.user.BeanResourceMenuRole;
import com.harmonycloud.zeus.dao.user.BeanResourceMenuRoleMapper;
import com.harmonycloud.zeus.service.user.ResourceMenuRoleService;
import lombok.extern.slf4j.Slf4j;
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
            new QueryWrapper<BeanResourceMenuRole>().eq("role_id", roleId).eq("available", 1);
        return beanResourceMenuRoleMapper.selectList(rmRoleWrapper);
    }
}
