package com.harmonycloud.zeus.service.user.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.model.user.ResourceMenuDto;
import com.harmonycloud.caas.common.model.user.RoleDto;
import com.harmonycloud.caas.common.model.user.UserRole;
import com.harmonycloud.caas.filters.token.JwtTokenComponent;
import com.harmonycloud.caas.filters.user.CurrentUser;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.zeus.bean.user.BeanResourceMenu;
import com.harmonycloud.zeus.bean.user.BeanResourceMenuRole;
import com.harmonycloud.zeus.service.user.ResourceMenuRoleService;
import com.harmonycloud.zeus.service.user.ResourceMenuService;
import com.harmonycloud.zeus.dao.user.BeanRoleMapper;
import com.harmonycloud.zeus.service.user.RoleService;
import com.harmonycloud.zeus.service.user.UserRoleService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
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
    @Autowired
    private UserRoleService userRoleService;

    @Override
    public void add(RoleDto roleDto) {
        // todo 校验角色名称是否已存在
        // 获取当前用户的角色id
        CurrentUser currentUser = CurrentUserRepository.getUser();
        String currentRoleId = JwtTokenComponent.checkToken(currentUser.getToken()).getValue().getString("roleId");
        List<BeanResourceMenuRole> rmRoleList = resourceMenuRoleService.list(currentRoleId);
        //判断创建角色的权限是否是当前登陆用户角色权限的子集
        if (!roleDto.getMenu().stream().map(ResourceMenuDto::getId).collect(Collectors.toList())
            .containsAll(rmRoleList.stream().map(BeanResourceMenuRole::getId).collect(Collectors.toList()))) {
            //todo
            throw new BusinessException(ErrorMessage.NOT_FOUND);
        }
        BeanRole beanRole = new BeanRole();
        BeanUtils.copyProperties(roleDto, beanRole);
        beanRole.setStatus(true);
        beanRole.setCreateTime(new Date());
        beanRoleMapper.insert(beanRole);
        //绑定角色菜单权限
        bind(roleDto);
    }

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
        List<Integer> currentRoleMenuList = rmRoleMap.get(Integer.valueOf(currentRoleId)).stream()
            .map(BeanResourceMenuRole::getResourceMenuId).collect(Collectors.toList());
        // 过滤权限为当前用户子权限的角色
        beanRoleList = beanRoleList.stream()
            .filter(role -> currentRoleMenuList.containsAll(rmRoleMap.get(role.getId()).stream()
                .map(BeanResourceMenuRole::getResourceMenuId).collect(Collectors.toList())))
            .collect(Collectors.toList());
        return beanRoleList.stream().map(beanRole -> {
            RoleDto roleDto = new RoleDto();
            BeanUtils.copyProperties(beanRole, roleDto);
            return roleDto;
        }).filter(roleDto -> roleDto.getName().contains(key) || roleDto.getDescription().contains(key))
            .collect(Collectors.toList());
    }

    @Override
    public void update(RoleDto roleDto) {

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
        if (CollectionUtils.isEmpty(userRoleList)){
            //todo
            throw new BusinessException(ErrorMessage.NOT_FOUND);
        }
    }
}
