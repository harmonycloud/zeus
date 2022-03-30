package com.harmonycloud.zeus.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.zeus.bean.user.BeanRoleAuthority;
import com.harmonycloud.zeus.dao.user.BeanRoleAuthorityMapper;
import com.harmonycloud.zeus.service.user.RoleAuthorityService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * @author xutianhong
 * @Date 2022/3/28 5:21 下午
 */
@Service
@Slf4j
public class RoleAuthorityServiceImpl implements RoleAuthorityService {

    @Autowired
    private BeanRoleAuthorityMapper beanRoleAuthorityMapper;

    @Override
    public void insert(Integer roleId, String type, String power) {
        BeanRoleAuthority beanRoleAuthority = new BeanRoleAuthority();
        beanRoleAuthority.setRoleId(roleId);
        beanRoleAuthority.setType(type);
        beanRoleAuthority.setPower(power);
        beanRoleAuthorityMapper.insert(beanRoleAuthority);
    }

    @Override
    public void delete(Integer roleId) {
        QueryWrapper<BeanRoleAuthority> wrapper = new QueryWrapper<BeanRoleAuthority>().eq("role_id", roleId);
        beanRoleAuthorityMapper.delete(wrapper);
    }

    @Override
    public List<BeanRoleAuthority> list(Integer roleId) {
        QueryWrapper<BeanRoleAuthority> wrapper = new QueryWrapper<>();
        if (roleId != null){
            wrapper.eq("role_id", roleId);
        }
        return beanRoleAuthorityMapper.selectList(wrapper);
    }

    @Override
    public void update(Integer roleId, Map<String, String> power) {
        QueryWrapper<BeanRoleAuthority> wrapper = new QueryWrapper<BeanRoleAuthority>().eq("role_id", roleId);
        List<BeanRoleAuthority> beanRoleAuthorityList = beanRoleAuthorityMapper.selectList(wrapper);
        beanRoleAuthorityList.forEach(beanRoleAuthority -> {
            beanRoleAuthority.setPower(power.get(beanRoleAuthority.getType()));
            beanRoleAuthorityMapper.updateById(beanRoleAuthority);
        });
    }
}
