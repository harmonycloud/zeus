package com.harmonycloud.zeus.service.user;

import com.harmonycloud.caas.common.enums.middleware.MiddlewareOfficialNameEnum;
import com.harmonycloud.caas.common.model.user.ResourceMenuDto;
import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.caas.common.model.user.UserRole;
import com.harmonycloud.caas.filters.token.JwtTokenComponent;
import com.harmonycloud.caas.filters.user.CurrentUser;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.util.RequestUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.user.UserConstant.USERNAME;

/**
 * @author liyinlong
 * @since 2022/6/9 5:01 下午
 */
@Slf4j
public abstract class AbstractUserService implements UserService{

    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private MiddlewareService middlewareService;
    @Autowired
    private RoleService roleService;

    public String getUsername(){
        CurrentUser currentUser = CurrentUserRepository.getUser();
        return currentUser.getUsername();
    }

    /**
     * 查询菜单信息
     * @param clusterId
     * @return
     */
    @Override
    public List<ResourceMenuDto> menu(String clusterId) {
        CurrentUser currentUser = CurrentUserRepository.getUser();
        String username = JwtTokenComponent.checkToken(currentUser.getToken()).getValue().getString(USERNAME);
        UserDto userDto = getUserDto(username);
        String projectId = RequestUtil.getProjectId();
        List<ResourceMenuDto> resourceMenuDtoList = roleService.listMenuByRoleId(userDto);

        Map<Integer, List<ResourceMenuDto>> resourceMenuDtoMap =
                resourceMenuDtoList.stream().collect(Collectors.groupingBy(ResourceMenuDto::getParentId));
        List<ResourceMenuDto> firstMenuList = resourceMenuDtoMap.get(0);
        resourceMenuDtoMap.remove(0);
        firstMenuList.forEach(firstMenu -> {
            if (!resourceMenuDtoMap.containsKey(firstMenu.getId())) {
                return;
            }
            firstMenu.setSubMenu(resourceMenuDtoMap.get(firstMenu.getId()));
            Collections.sort(firstMenu.getSubMenu());
        });
        if (StringUtils.isNotBlank(clusterId)) {
            Map<String, String> power = new HashMap<>();
            if (!userDto.getIsAdmin() && StringUtils.isNotEmpty(projectId)) {
                power.putAll(
                        userDto.getUserRoleList().stream().filter(userRole -> userRole.getProjectId().equals(projectId))
                                .collect(Collectors.toList()).get(0).getPower());
            }
            setServiceMenuSubMenu(firstMenuList, clusterId, power);
        }
        Collections.sort(firstMenuList);
        return firstMenuList;
    }

    /**
     * @description 将中间件设为服务列表菜单的子菜单
     * @author  liyinlong
     * @since 2021/11/2 4:09 下午
     */
    public void setServiceMenuSubMenu(List<ResourceMenuDto> menuDtoList, String clusterId, Map<String, String> power) {
        menuDtoList.forEach(parentMenu -> {
            if ("serviceList".equals(parentMenu.getName())) {
                List<ResourceMenuDto> resourceMenuDtos = middlewareService.listAllMiddlewareAsMenu(clusterId);
                if (!CollectionUtils.isEmpty(power)) {
                    resourceMenuDtos = resourceMenuDtos.stream()
                            .filter(resourceMenuDto -> power.keySet().stream()
                                    .anyMatch(key -> !"0000".equals(power.get(key)) && key.equals(resourceMenuDto.getName())))
                            .collect(Collectors.toList());
                }
                resourceMenuDtos.forEach(resourceMenuDto -> {
                    resourceMenuDto
                            .setAliasName(MiddlewareOfficialNameEnum.findByMiddlewareName(resourceMenuDto.getAliasName()));
                    resourceMenuDto.setUrl(
                            parentMenu.getUrl() + "/" + resourceMenuDto.getName() + "/" + resourceMenuDto.getAliasName());
                });
                parentMenu.setSubMenu(resourceMenuDtos);
            }
        });
    }

    /**
     * 设置用户的角色列表
     * @param userName 用户名
     * @param userDto
     */
    public void setUserRoleList(String userName, UserDto userDto) {
        List<UserRole> userRoleList = userRoleService.get(userName);
        if (!CollectionUtils.isEmpty(userRoleList)) {
            userDto.setUserRoleList(userRoleList);
            userDto.setIsAdmin(userRoleList.stream().anyMatch(userRole -> userRole.getRoleId() == 1));
        }
    }

}
