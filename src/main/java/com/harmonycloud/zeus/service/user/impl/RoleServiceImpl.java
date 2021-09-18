package com.harmonycloud.zeus.service.user.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.user.ResourceMenuDto;
import com.harmonycloud.caas.common.model.user.RoleDto;
import com.harmonycloud.caas.common.model.user.UserRole;
import com.harmonycloud.caas.filters.token.JwtTokenComponent;
import com.harmonycloud.caas.filters.user.CurrentUser;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.zeus.bean.user.BeanResourceMenuRole;
import com.harmonycloud.zeus.bean.user.BeanRole;
import com.harmonycloud.zeus.dao.user.BeanRoleMapper;
import com.harmonycloud.zeus.service.user.ResourceMenuRoleService;
import com.harmonycloud.zeus.service.user.ResourceMenuService;
import com.harmonycloud.zeus.service.user.RoleService;
import com.harmonycloud.zeus.service.user.UserRoleService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2021/7/27 2:54 下午
 */
@Slf4j
@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private BeanRoleMapper beanRoleMapper;
    @Autowired
    private ResourceMenuRoleService resourceMenuRoleService;
    @Autowired
    private ResourceMenuService resourceMenuService;
    @Autowired
    private UserRoleService userRoleService;

    @Override
    public void add(RoleDto roleDto) {
        // 校验角色名称是否已存在
        if (checkExist(roleDto.getName())) {
            throw new BusinessException(ErrorMessage.ROLE_EXIST);
        }
        if (!checkMenu(roleDto)) {
            throw new BusinessException(ErrorMessage.CREATE_ROLE_FAILED);
        }
        BeanRole beanRole = new BeanRole();
        BeanUtils.copyProperties(roleDto, beanRole);
        beanRole.setStatus(true);
        beanRole.setCreateTime(new Date());
        CurrentUser currentUser = CurrentUserRepository.getUser();
        String currentRoleId = JwtTokenComponent.checkToken(currentUser.getToken()).getValue().getString("roleId");
        beanRole.setParent(Integer.parseInt(currentRoleId));
        beanRoleMapper.insert(beanRole);
        // 绑定角色菜单权限
        bind(roleDto);
    }

    @Override
    public RoleDto get(Integer roleId) {
        // 获取角色信息
        QueryWrapper<BeanRole> roleWrapper = new QueryWrapper<BeanRole>().eq("id", roleId);
        BeanRole beanRole = beanRoleMapper.selectOne(roleWrapper);
        if (ObjectUtils.isEmpty(beanRole)) {
            throw new BusinessException(ErrorMessage.ROLE_NOT_EXIST);
        }
        RoleDto role = new RoleDto();
        BeanUtils.copyProperties(beanRole, role);
        return role;
    }

    @Override
    public void delete(Integer roleId) {
        //校验角色是否有绑定用户
        checkUserBind(roleId);
        //删除角色
        beanRoleMapper.deleteById(roleId);
        //删除角色菜单绑定
        resourceMenuRoleService.delete(roleId);
    }

    @Override
    public List<RoleDto> list(String key) {
        // 获取当前用户的角色id
        CurrentUser currentUser = CurrentUserRepository.getUser();
        String currentRoleId = JwtTokenComponent.checkToken(currentUser.getToken()).getValue().getString("roleId");
        // 获取所有角色信息
        QueryWrapper<BeanRole> roleWrapper = new QueryWrapper<>();
        List<BeanRole> beanRoleList = beanRoleMapper.selectList(roleWrapper);
        // 获取所有角色菜单(权限)映照
        List<BeanResourceMenuRole> beanResourceMenuRoleList = resourceMenuRoleService.list(null);
        Map<Integer, List<BeanResourceMenuRole>> rmRoleMap =
            beanResourceMenuRoleList.stream().collect(Collectors.groupingBy(BeanResourceMenuRole::getRoleId));
        //获取当前角色的菜单权限
        List<Integer> currentRoleMenuList = rmRoleMap.get(Integer.valueOf(currentRoleId)).stream()
            .map(BeanResourceMenuRole::getResourceMenuId).collect(Collectors.toList());
        // 过滤权限为当前用户子权限的角色
        beanRoleList = beanRoleList.stream().filter(role -> {
            if (rmRoleMap.containsKey(role.getId())) {
                return currentRoleMenuList.containsAll(rmRoleMap.get(role.getId()).stream()
                    .map(BeanResourceMenuRole::getResourceMenuId).collect(Collectors.toList()));
            } else {
                rmRoleMap.put(role.getId(), new ArrayList<>());
                return true;
            }
        }).collect(Collectors.toList());
        return beanRoleList.stream().map(beanRole -> {
            RoleDto roleDto = new RoleDto();
            BeanUtils.copyProperties(beanRole, roleDto);
            roleDto.setMenu(resourceMenuService.list(rmRoleMap.get(beanRole.getId()).stream()
                .map(BeanResourceMenuRole::getResourceMenuId).collect(Collectors.toList())));
            return roleDto;
        }).filter(roleDto -> {
            if (StringUtils.isNotEmpty(key)) {
                return roleDto.getName().contains(key) || roleDto.getDescription().contains(key);
            }
            return true;
        }).collect(Collectors.toList());
    }

    @Override
    public void update(RoleDto roleDto) {
        if (!checkMenu(roleDto)) {
            throw new BusinessException(ErrorMessage.UPDATE_ROLE_FAILED);
        }
        // 更新角色信息
        BeanRole beanRole = new BeanRole();
        beanRole.setId(roleDto.getId());
        beanRole.setName(roleDto.getName());
        beanRole.setDescription(roleDto.getDescription());
        beanRoleMapper.updateById(beanRole);
        // 更新角色权限
        roleDto.getMenu().forEach(menu -> resourceMenuRoleService.update(roleDto.getId(), menu.getId(), menu.getOwn()));
    }

    @Override
    public List<ResourceMenuDto> listMenuByRoleId(String roleId) {
        // 获取角色菜单映照
        List<BeanResourceMenuRole> beanResourceMenuRoleList = resourceMenuRoleService.list(roleId);
        // 获取菜单信息
        List<ResourceMenuDto> resourceMenuDtoList = resourceMenuService.list(beanResourceMenuRoleList.stream()
            .map(BeanResourceMenuRole::getResourceMenuId).collect(Collectors.toList()));
        return resourceMenuDtoList.stream().filter(ResourceMenuDto::getOwn).collect(Collectors.toList());
    }

    /**
     * 校验角色名是否存在
     */
    public boolean checkExist(String name){
        QueryWrapper<BeanRole> wrapper = new QueryWrapper<BeanRole>().eq("name", name);
        BeanRole beanRole = beanRoleMapper.selectOne(wrapper);
        return !ObjectUtils.isEmpty(beanRole);
    }

    /**
     * 校验角色菜单权限
     */
    public boolean checkMenu(RoleDto roleDto) {
        // 获取当前用户的角色id
        CurrentUser currentUser = CurrentUserRepository.getUser();
        String currentRoleId = JwtTokenComponent.checkToken(currentUser.getToken()).getValue().getString("roleId");
        List<BeanResourceMenuRole> rmRoleList = resourceMenuRoleService.list(currentRoleId);
        // 判断创建角色的权限是否是当前登陆用户角色权限的子集
        if (!rmRoleList.stream().map(BeanResourceMenuRole::getResourceMenuId).collect(Collectors.toList())
            .containsAll(roleDto.getMenu().stream().filter(ResourceMenuDto::getOwn).map(ResourceMenuDto::getId).collect(Collectors.toList()))) {
            log.error("创建/更新 角色的权限不能大于当前登陆用户的角色权限");
            return false;
        }
        // 判断更新的角色的权限，是否大于该角色曾创建过的角色的权限的最大集合
        if (roleDto.getId() != null) {
            QueryWrapper<BeanRole> wrapper = new QueryWrapper<BeanRole>().eq("parent", roleDto.getId());
            List<BeanRole> beanRoleList = beanRoleMapper.selectList(wrapper);
            if (!CollectionUtils.isEmpty(beanRoleList)) {
                for (BeanRole beanRole : beanRoleList) {
                    List<BeanResourceMenuRole> list = resourceMenuRoleService.list(beanRole.getId().toString());
                    if (!roleDto.getMenu().stream().map(ResourceMenuDto::getId).collect(Collectors.toList())
                        .containsAll(
                            list.stream().map(BeanResourceMenuRole::getResourceMenuId).collect(Collectors.toList()))) {
                        log.error("更新角色的权限不能小于该角色曾创建过的角色的权限的最大集合");
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * 绑定角色菜单权限
     */
    private void bind(RoleDto roleDto) {
        QueryWrapper<BeanRole> wrapper = new QueryWrapper<BeanRole>().eq("name", roleDto.getName()).eq("status", true);
        BeanRole beanRole = beanRoleMapper.selectOne(wrapper);
        roleDto.getMenu().forEach(menu -> resourceMenuRoleService.add(beanRole.getId(), menu.getId(), menu.getOwn()));
    }

    /**
     * 校验角色用户绑定
     */
    public void checkUserBind(Integer roleId){
        List<UserRole> userRoleList = userRoleService.findByRoleId(roleId);
        if (!CollectionUtils.isEmpty(userRoleList)){
            throw new BusinessException(ErrorMessage.ROLE_HAS_BEEN_BOUND);
        }
    }
}
