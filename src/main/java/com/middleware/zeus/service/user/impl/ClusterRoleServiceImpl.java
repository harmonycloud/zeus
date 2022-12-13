package com.middleware.zeus.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.middleware.Namespace;
import com.middleware.zeus.bean.user.BeanClusterRole;
import com.middleware.zeus.dao.user.BeanClusterRoleMapper;
import com.middleware.zeus.service.user.ClusterRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2021/12/14 10:05 上午
 */
@Slf4j
@Service
public class ClusterRoleServiceImpl implements ClusterRoleService {

    @Autowired
    private BeanClusterRoleMapper beanClusterRoleMapper;

    @Override
    public void add(Integer roleId, List<MiddlewareClusterDTO> clusterList) {
        clusterList.forEach(cluster -> cluster.getNamespaceList().forEach(namespace -> {
            BeanClusterRole beanClusterRole = new BeanClusterRole();
            beanClusterRole.setRoleId(roleId);
            beanClusterRole.setClusterId(cluster.getId());
            beanClusterRole.setNamespace(namespace.getName());
            beanClusterRoleMapper.insert(beanClusterRole);
        }));
    }

    @Override
    public void update(Integer roleId, List<MiddlewareClusterDTO> clusterList) {
        QueryWrapper<BeanClusterRole> wrapper = new QueryWrapper<BeanClusterRole>().eq("role_id", roleId);
        List<BeanClusterRole> beanClusterRoleList = beanClusterRoleMapper.selectList(wrapper);
        // 过滤不需要做修改的
        if (!CollectionUtils.isEmpty(beanClusterRoleList)) {
            clusterList.forEach(cluster -> {
                if (CollectionUtils.isEmpty(cluster.getNamespaceList())){
                    throw new BusinessException(ErrorMessage.ROLE_NAMESPACE_PERMISSION_EMPTY);
                }
                List<Namespace> tempNamespaceList = new ArrayList<>(cluster.getNamespaceList());
                tempNamespaceList.forEach(namespace -> {
                    if (beanClusterRoleList
                        .removeIf(beanClusterRole -> beanClusterRole.getClusterId().equals(cluster.getId())
                            && beanClusterRole.getNamespace().equals(namespace.getName()))) {
                        cluster.getNamespaceList().remove(namespace);
                    }
                });
            });
            // 删除不再给予权限的
            if (!CollectionUtils.isEmpty(beanClusterRoleList)) {
                beanClusterRoleList.forEach(beanClusterRole -> beanClusterRoleMapper.deleteById(beanClusterRole));
            }
        }
        this.add(roleId, clusterList);
    }

    @Override
    public void delete(Integer roleId) {
        QueryWrapper<BeanClusterRole> wrapper = new QueryWrapper<BeanClusterRole>().eq("role_id", roleId);
        beanClusterRoleMapper.delete(wrapper);
    }

    @Override
    public List<MiddlewareClusterDTO> get(Integer roleId) {
        QueryWrapper<BeanClusterRole> wrapper = new QueryWrapper<BeanClusterRole>().eq("role_id", roleId);
        List<BeanClusterRole> beanClusterRoleList = beanClusterRoleMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(beanClusterRoleList)){
            return new ArrayList<>();
        }
        Map<String, List<BeanClusterRole>> clusterRoleListMap = beanClusterRoleList.stream().collect(Collectors.groupingBy(BeanClusterRole::getClusterId));
        List<MiddlewareClusterDTO> clusterList = new ArrayList<>();
        for (String clusterId : clusterRoleListMap.keySet()){
            MiddlewareClusterDTO cluster = new MiddlewareClusterDTO();
            cluster.setId(clusterId);
            cluster.setName(clusterId.split("--")[1]);
            List<Namespace> namespaceList = clusterRoleListMap.get(clusterId).stream()
                    .map(beanClusterRole -> new Namespace().setName(beanClusterRole.getNamespace()))
                    .collect(Collectors.toList());
            cluster.setNamespaceList(namespaceList);
            clusterList.add(cluster);
        }
        return clusterList;
    }

    @Override
    public MiddlewareClusterDTO get(Integer roleId, String clusterId) {
        List<MiddlewareClusterDTO> clusterList =
            this.get(roleId).stream().filter(cluster -> cluster.getId().equals(clusterId)).collect(Collectors.toList());
        if (CollectionUtils.isEmpty(clusterList)) {
            return null;
        }
        return clusterList.get(0);
    }
}
