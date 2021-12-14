package com.harmonycloud.zeus.service.user.impl;

import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.zeus.bean.user.BeanClusterRole;
import com.harmonycloud.zeus.dao.user.BeanClusterRoleMapper;
import com.harmonycloud.zeus.service.user.ClusterRoleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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
            beanClusterRole.setId(roleId);
            beanClusterRole.setClusterId(cluster.getId());
            beanClusterRole.setNamespace(namespace.getName());
            beanClusterRoleMapper.insert(beanClusterRole);
        }));
    }

    @Override
    public void update(Integer roleId, List<MiddlewareClusterDTO> clusterList) {

    }

    @Override
    public void delete(Integer roleId) {

    }

    @Override
    public List<MiddlewareClusterDTO> get(Integer roleId) {
        return null;
    }
}
