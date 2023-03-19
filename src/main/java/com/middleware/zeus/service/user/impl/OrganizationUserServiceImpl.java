package com.middleware.zeus.service.user.impl;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.zeus.bean.user.BeanOrganizationUser;
import com.middleware.zeus.dao.user.BeanOrganizationUserMapper;
import com.middleware.zeus.service.user.OrganizationUserService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2023/3/10 2:47 下午
 */
@Service
@Slf4j
public class OrganizationUserServiceImpl implements OrganizationUserService {

    @Autowired
    private BeanOrganizationUserMapper beanOrganizationUserMapper;

    @Override
    public List<BeanOrganizationUser> list(String organId) {
        QueryWrapper<BeanOrganizationUser> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(organId)) {
            wrapper.eq("organ_id", organId);
        }
        return beanOrganizationUserMapper.selectList(wrapper);
    }

    @Override
    public List<BeanOrganizationUser> listByUsername(String username) {
        QueryWrapper<BeanOrganizationUser> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(username)) {
            wrapper.eq("username", username);
        }
        return beanOrganizationUserMapper.selectList(wrapper);
    }

    @Override
    public void insert(String organId, String username, Integer roleId) {
        if (checkExist(organId, username)) {
            throw new BusinessException(ErrorMessage.NOT_EXIST);
        }
        BeanOrganizationUser beanOrganizationUser = new BeanOrganizationUser();
        beanOrganizationUser.setOrganId(organId);
        beanOrganizationUser.setUsername(username);
        beanOrganizationUser.setRoleId(roleId);
        beanOrganizationUserMapper.insert(beanOrganizationUser);
    }

    @Override
    public void update(String organId, String username, Integer roleId) {
        if (!checkExist(organId, username)) {
            throw new BusinessException(ErrorMessage.NOT_EXIST);
        }
        QueryWrapper<BeanOrganizationUser> wrapper =
            new QueryWrapper<BeanOrganizationUser>().eq("organ_id", organId).eq("username", username);
        BeanOrganizationUser beanOrganizationUser = new BeanOrganizationUser();
        beanOrganizationUser.setOrganId(organId);
        beanOrganizationUser.setUsername(username);
        beanOrganizationUser.setRoleId(roleId);

        beanOrganizationUserMapper.update(beanOrganizationUser, wrapper);
    }

    @Override
    public void delete(String organId, String username) {
        QueryWrapper<BeanOrganizationUser> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(organId)) {
            wrapper.eq("organ_id", organId);
        }
        if (StringUtils.isNotEmpty(username)) {
            wrapper.eq("username", username);
        }
        beanOrganizationUserMapper.delete(wrapper);
    }

    public Boolean checkExist(String organId, String username) {
        QueryWrapper<BeanOrganizationUser> wrapper =
            new QueryWrapper<BeanOrganizationUser>().eq("organ_id", organId).eq("username", username);
        List<BeanOrganizationUser> list = beanOrganizationUserMapper.selectList(wrapper);
        return !CollectionUtils.isEmpty(list);
    }

}
