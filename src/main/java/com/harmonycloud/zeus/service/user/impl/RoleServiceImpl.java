package com.harmonycloud.zeus.service.user.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.model.user.ResourceMenuDto;
import com.harmonycloud.zeus.bean.user.BeanResourceMenu;
import com.harmonycloud.zeus.bean.user.BeanResourceMenuRole;
import com.harmonycloud.zeus.service.user.ResourceMenuRoleService;
import com.harmonycloud.zeus.service.user.ResourceMenuService;
import com.harmonycloud.zeus.dao.user.BeanRoleMapper;
import com.harmonycloud.zeus.service.user.RoleService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.zeus.bean.user.BeanRole;

import static com.harmonycloud.caas.common.constants.NameConstant.ADMIN;

/**
 * @author xutianhong
 * @Date 2021/7/27 2:54 下午
 */
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private BeanRoleMapper beanRoleMapper;
    @Autowired
    private ResourceMenuRoleService resourceMenuRoleService;
    @Autowired
    private ResourceMenuService resourceMenuService;

    @Override
    public BeanRole get(Integer roleId) throws Exception {
        // 获取角色信息
        QueryWrapper<BeanRole> roleWrapper = new QueryWrapper<BeanRole>().eq("id", roleId);
        BeanRole beanRole = beanRoleMapper.selectOne(roleWrapper);
        if (ObjectUtils.isEmpty(beanRole)) {
            throw new BusinessException(ErrorMessage.ROLE_NOT_EXIST);
        }
        return beanRole;
    }

    @Override
    public List<BeanRole> list(Boolean withAdmin) {
        // 获取所有角色信息
        QueryWrapper<BeanRole> roleWrapper = new QueryWrapper<>();
        List<BeanRole> beanRoleList = beanRoleMapper.selectList(roleWrapper);
        if (!withAdmin){
            beanRoleList = beanRoleList.stream().filter(beanRole -> !beanRole.getName().equals(ADMIN)).collect(Collectors.toList());
        }
        return beanRoleList;
    }

    @Override
    public List<ResourceMenuDto> listMenuByRoleId(String roleId) {
        // 获取角色菜单映照
        List<BeanResourceMenuRole> beanResourceMenuRoleList = resourceMenuRoleService.list(roleId);
        // 获取菜单信息
        List<BeanResourceMenu> beanResourceMenuList = resourceMenuService.list(beanResourceMenuRoleList.stream()
            .map(BeanResourceMenuRole::getResourceMenuId).collect(Collectors.toList()));
        // 封装数据
        return beanResourceMenuList.stream().map(beanResourceMenu -> {
            ResourceMenuDto resourceMenuDto = new ResourceMenuDto();
            BeanUtils.copyProperties(beanResourceMenu, resourceMenuDto);
            return resourceMenuDto;
        }).collect(Collectors.toList());
    }
}
