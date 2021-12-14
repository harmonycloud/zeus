package com.harmonycloud.zeus.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.zeus.bean.user.BeanClusterRole;
import com.harmonycloud.zeus.dao.user.BeanClusterRoleMapper;
import com.harmonycloud.zeus.service.user.ClusterRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

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
            // 添加新的权限
            this.add(roleId, clusterList);
        }
    }

    @Override
    public void delete(Integer roleId) {
        QueryWrapper<BeanClusterRole> wrapper = new QueryWrapper<BeanClusterRole>().eq("role_id", roleId);
        beanClusterRoleMapper.delete(wrapper);
    }

    @Override
    public List<MiddlewareClusterDTO> get(Integer roleId) {
        return null;
    }

}
