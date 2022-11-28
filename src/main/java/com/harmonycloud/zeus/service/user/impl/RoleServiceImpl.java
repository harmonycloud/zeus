package com.harmonycloud.zeus.service.user.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;
import com.harmonycloud.zeus.bean.user.BeanRoleAuthority;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;
import com.harmonycloud.zeus.service.user.*;
import com.harmonycloud.zeus.util.RequestUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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

import lombok.extern.slf4j.Slf4j;

import static com.harmonycloud.caas.common.constants.CommonConstant.*;

/**
 * @author xutianhong
 * @Date 2021/7/27 2:54 下午
 */
@Slf4j
@Service
public class RoleServiceImpl implements RoleService {

    @Value("${system.disasterRecovery.enable:true}")
    private String disasterEnable;
    @Value("${system.disasterRecovery.menu-id:10}")
    private String disasterMenuId;

    @Autowired
    private BeanRoleMapper beanRoleMapper;
    @Autowired
    private ResourceMenuService resourceMenuService;
    @Autowired
    private ResourceMenuRoleService resourceMenuRoleService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private ClusterRoleService clusterRoleService;
    @Autowired
    private RoleAuthorityService roleAuthorityService;
    @Autowired
    private MiddlewareInfoService middlewareInfoService;

    @Override
    public void add(RoleDto roleDto) {
        // 校验角色名称是否已存在
        if (checkExist(roleDto.getName())) {
            throw new BusinessException(ErrorMessage.ROLE_EXIST);
        }
        BeanRole beanRole = new BeanRole();
        BeanUtils.copyProperties(roleDto, beanRole);
        beanRole.setStatus(true);
        beanRole.setCreateTime(new Date());
        beanRoleMapper.insert(beanRole);
        // 获取id
        QueryWrapper<BeanRole> wrapper = new QueryWrapper<BeanRole>().eq("name", roleDto.getName()).eq("status", true);
        BeanRole exit = beanRoleMapper.selectOne(wrapper);
        roleDto.setId(exit.getId());
        // 绑定角色权限
        initRolePower(roleDto);
        bindRolePower(roleDto);
        // 初始化菜单权限
        initResourceMenuRole(roleDto);
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
        //删除角色权限绑定
        roleAuthorityService.delete(roleId);
        //删除角色集群绑定
        clusterRoleService.delete(roleId);
        // 删除角色菜单绑定关系
        resourceMenuRoleService.delete(roleId);
    }

    @Override
    public List<RoleDto> list(String key) {
        // 获取所有角色信息
        QueryWrapper<BeanRole> roleWrapper = new QueryWrapper<>();
        List<BeanRole> beanRoleList = beanRoleMapper.selectList(roleWrapper);
        // 获取所有角色(权限)映照
        List<BeanRoleAuthority> beanRoleAuthorityList = roleAuthorityService.list(null);
        Map<Integer, List<BeanRoleAuthority>> beanRoleAuthorityListMap =
            beanRoleAuthorityList.stream().collect(Collectors.groupingBy(BeanRoleAuthority::getRoleId));
        // 封装返还数据，并根据关键词过滤
        return beanRoleList.stream().map(beanRole -> {
            RoleDto roleDto = new RoleDto();
            BeanUtils.copyProperties(beanRole, roleDto);
            roleDto.setPower(beanRoleAuthorityListMap.get(roleDto.getId()).stream()
                .collect(Collectors.toMap(BeanRoleAuthority::getType, BeanRoleAuthority::getPower)));
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
        // 更新角色信息
        BeanRole beanRole = new BeanRole();
        beanRole.setId(roleDto.getId());
        beanRole.setName(roleDto.getName());
        beanRole.setDescription(roleDto.getDescription());
        beanRoleMapper.updateById(beanRole);
        // 更新角色权限
        if (!CollectionUtils.isEmpty(roleDto.getPower())) {
            roleAuthorityService.update(roleDto.getId(), roleDto.getPower());
        }
    }

    @Override
    public List<ResourceMenuDto> listMenuByRoleId(UserDto userDto, String projectId) {
        List<BeanResourceMenuRole> list;
        if (Boolean.TRUE.equals(!userDto.getIsAdmin()) && StringUtils.isNotEmpty(projectId)) {
            Integer roleId =
                userDto.getUserRoleList().stream().filter(userRole -> userRole.getProjectId().equals(projectId))
                    .collect(Collectors.toList()).get(0).getRoleId();
            list = resourceMenuRoleService.list(String.valueOf(roleId));
        } else {
            list = resourceMenuRoleService.listAdminMenu();
        }
        // 过滤是否开启灾备服务
        if (!Boolean.parseBoolean(disasterEnable)) {
            list = list.stream().filter(menuDto -> menuDto.getId() != Integer.parseInt(disasterMenuId))
                .collect(Collectors.toList());
        }
        List<Integer> ids = list.stream().map(BeanResourceMenuRole::getId).collect(Collectors.toList());
        // 获取菜单信息
        List<ResourceMenuDto> resourceMenuDtoList = resourceMenuService.list(ids);
        return resourceMenuDtoList.stream().filter(ResourceMenuDto::getOwn).collect(Collectors.toList());
    }

    @Override
    public void initMiddlewareAuthority(String type) {
        if (roleAuthorityService.checkExistByType(type)){
            return;
        }
        QueryWrapper<BeanRole> wrapper = new QueryWrapper<>();
        List<BeanRole> beanRoleList = beanRoleMapper.selectList(wrapper);
        for (BeanRole beanRole : beanRoleList) {
            if (beanRole.getId().equals(NUM_ONE) || beanRole.getId().equals(NUM_TWO)
                || beanRole.getId().equals(NUM_THREE)) {
                roleAuthorityService.insert(beanRole.getId(), type, "1111");
            } else if (beanRole.getId().equals(NUM_FOUR)) {
                roleAuthorityService.insert(beanRole.getId(), type, "1000");
            } else {
                roleAuthorityService.insert(beanRole.getId(), type, "0000");
            }
        }
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
     * 绑定角色权限
     */
    private void bindRolePower(RoleDto roleDto) {
        for (String key : roleDto.getPower().keySet()){
            roleAuthorityService.insert(roleDto.getId(), key, roleDto.getPower().get(key));
        }
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

    public void initRolePower(RoleDto roleDto){
        List<BeanMiddlewareInfo> beanMiddlewareInfoList = middlewareInfoService.list(false);
        Map<String, String> power = new HashMap<>();
        beanMiddlewareInfoList.forEach(beanMiddlewareInfo -> power.put(beanMiddlewareInfo.getChartName(), "0000"));
        roleDto.setPower(power);
    }

    public void initResourceMenuRole(RoleDto roleDto){
        resourceMenuRoleService.init(roleDto.getId());
        resourceMenuRoleService.add(roleDto.getId(), 3, true);
        resourceMenuRoleService.add(roleDto.getId(), 4, true);
    }
}
