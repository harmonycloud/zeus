package com.harmonycloud.zeus.service.user;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.zeus.bean.user.BeanProject;
import com.harmonycloud.zeus.bean.user.BeanProjectNamespace;
import com.harmonycloud.zeus.dao.user.BeanProjectMapper;
import com.harmonycloud.zeus.dao.user.BeanProjectNamespaceMapper;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.util.AssertUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2022/6/14 5:27 下午
 */
@Slf4j
public abstract class AbstractProjectService implements ProjectService{

    @Autowired
    private BeanProjectNamespaceMapper beanProjectNamespaceMapper;
    @Autowired
    private BeanProjectMapper beanProjectMapper;
    @Autowired
    private ClusterService clusterService;

    @Override
    public void bindNamespace(Namespace namespace) {
        QueryWrapper<BeanProjectNamespace> wrapper =
                new QueryWrapper<BeanProjectNamespace>().eq("namespace", namespace.getName()).eq("cluster_id", namespace.getClusterId());
        List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(beanProjectNamespaceList)) {
            throw new BusinessException(ErrorMessage.PROJECT_NAMESPACE_ALREADY_BIND);
        }
        AssertUtil.notBlank(namespace.getProjectId(), DictEnum.PROJECT_ID);
        AssertUtil.notBlank(namespace.getName(), DictEnum.NAMESPACE_NAME);
        BeanProjectNamespace beanProjectNamespace = new BeanProjectNamespace();
        BeanUtils.copyProperties(namespace, beanProjectNamespace);
        beanProjectNamespace.setNamespace(namespace.getName());
        beanProjectNamespaceMapper.insert(beanProjectNamespace);
    }

    @Override
    public void bindNamespace(List<Namespace> namespaceList) {
        namespaceList.forEach(namespace -> {
            try {
                bindNamespace(namespace);
            } catch (Exception e) {
                log.error("绑定分区出错了", e);
            }
        });
    }

    @Override
    public void add(BeanProject beanProject) {
        beanProjectMapper.insert(beanProject);
    }

    @Override
    public List<String> getClusters(String projectId) {
        List<Namespace> namespaceList = this.getNamespace(projectId);
        return namespaceList.stream().map(Namespace::getClusterId).collect(Collectors.toList());
    }

    @Override
    public List<Namespace> getNamespace(String projectId) {
        QueryWrapper<BeanProjectNamespace> wrapper =
                new QueryWrapper<BeanProjectNamespace>().eq("project_id", projectId);
        List<BeanProjectNamespace> beanProjectNamespaceList = beanProjectNamespaceMapper.selectList(wrapper);

        return beanProjectNamespaceList.stream().map(beanProjectNamespace -> {
            Namespace namespace = new Namespace();
            BeanUtils.copyProperties(beanProjectNamespace, namespace);
            namespace.setClusterAliasName(clusterService.findById(namespace.getClusterId()).getNickname());
            namespace.setName(beanProjectNamespace.getNamespace());
            return namespace;
        }).collect(Collectors.toList());
    }

}
