package com.harmonycloud.zeus.service.user.impl;

import java.util.*;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;
import com.harmonycloud.zeus.bean.user.BeanRoleAuthority;
import com.harmonycloud.zeus.bean.user.BeanUser;
import com.harmonycloud.zeus.dao.user.BeanRoleAuthorityMapper;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;
import com.harmonycloud.zeus.service.user.*;
import com.harmonycloud.zeus.util.RequestUtil;
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

import lombok.extern.slf4j.Slf4j;

import static com.harmonycloud.caas.common.constants.CommonConstant.NUM_ONE;

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
    private ResourceMenuService resourceMenuService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private ClusterRoleService clusterRoleService;
    @Autowired
    private RoleAuthorityService roleAuthorityService;
    @Autowired
    private MiddlewareInfoService middlewareInfoService;
    @Autowired
    private UserService userService;

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
    public List<ResourceMenuDto> listMenuByRoleId(UserDto userDto) {
        String projectId = RequestUtil.getProjectId();
        List<Integer> ids;
        if (Boolean.TRUE.equals(!userDto.getIsAdmin()) && StringUtils.isNotEmpty(projectId)) {
            ids = Arrays.asList(3, 4);
            UserRole userRole = userDto.getUserRoleList().stream().filter(ur -> ur.getProjectId().equals(projectId))
                .collect(Collectors.toList()).get(0);
            for (String key : userRole.getPower().keySet()) {
                if (Integer.parseInt(userRole.getPower().get(key).split("")[1]) == 1) {
                    ids = Arrays.asList(3, 4, 5, 6, 7, 8, 11, 13, 14, 15);
                    break;
                }
            }
        } else {
            ids = Arrays.asList(1, 2, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21);
        }
        // 获取菜单信息
        List<ResourceMenuDto> resourceMenuDtoList = resourceMenuService.list(ids);
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
}
