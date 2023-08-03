package com.middleware.zeus.service.k8s.impl;

import com.middleware.zeus.integration.cluster.RoleBindingWrapper;
import com.middleware.zeus.service.k8s.RoleBindingService;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.rbac.RoleBinding;
import io.fabric8.kubernetes.api.model.rbac.RoleRef;
import io.fabric8.kubernetes.api.model.rbac.Subject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.middleware.zeus.common.constants.NameConstant.*;

/**
 * @author xutianhong
 * @Date 2023/7/25 8:10 下午
 */
@Slf4j
@Service
public class RoleBindingServiceImpl implements RoleBindingService {

    private static final String USER = "User";

    @Autowired
    private RoleBindingWrapper roleBindingWrapper;

    @Override
    public void bindUser(String clusterId, String namespace, String name, List<String> usernameList,
        String clusterRole) {
        // 查询roleBinding
        RoleBinding roleBinding = roleBindingWrapper.get(clusterId, namespace, name);
        // 当roleBinding不存在时，创建roleBinding
        if (roleBinding == null && StringUtils.isNotEmpty(clusterRole)) {
            this.create(clusterId, namespace, name, clusterRole);
            // 创建完成后重新获取
            roleBinding = roleBindingWrapper.get(clusterId, namespace, name);
        }
        if (roleBinding == null) {
            return;
        }
        // 更新roleBinding中的用户绑定
        List<Subject> subjectList = roleBinding.getSubjects();
        if (CollectionUtils.isEmpty(subjectList)) {
            subjectList = new ArrayList<>();
        }
        // 若已存在当前用户 先移除
        subjectList.removeIf(subject -> USER.equals(subject.getKind())
            && usernameList.stream().anyMatch(username -> username.equals(subject.getName())));

        // 添加用户
        for (String username : usernameList) {
            Subject subject = new Subject();
            subject.setApiGroup("rbac.authorization.k8s.io");
            subject.setKind(USER);
            subject.setName(username);
            subjectList.add(subject);
        }

        roleBinding.setSubjects(subjectList);

        // 更新用户
        roleBindingWrapper.update(clusterId, roleBinding);
    }

    @Override
    public void removeUser(String clusterId, String namespace, List<String> usernameList) {
        Map<String, String> labels = new HashMap<>();
        labels.put(APP, ZEUS);
        List<RoleBinding> roleBindingList = roleBindingWrapper.List(clusterId, namespace, labels);
        if (CollectionUtils.isEmpty(roleBindingList)) {
            return;
        }
        for (RoleBinding roleBinding : roleBindingList) {
            if (CollectionUtils.isEmpty(roleBinding.getSubjects())) {
                continue;
            }
            boolean update = roleBinding.getSubjects().removeIf(subject -> USER.equals(subject.getKind())
                && usernameList.stream().anyMatch(username -> username.equals(subject.getName())));
            if (update) {
                roleBindingWrapper.update(clusterId, roleBinding);
            }
        }
    }

    @Override
    public void create(String clusterId, String namespace, String name, String clusterRole) {
        RoleBinding roleBinding = new RoleBinding();
        ObjectMeta meta = new ObjectMeta();
        meta.setNamespace(namespace);
        meta.setName(name);

        Map<String, String> labels = new HashMap<>();
        labels.put(APP, ZEUS);
        meta.setLabels(labels);

        RoleRef roleRef = new RoleRef();
        roleRef.setKind(CLUSTER_ROLE);
        roleRef.setApiGroup("rbac.authorization.k8s.io");
        roleRef.setName(clusterRole);

        roleBinding.setMetadata(meta);
        roleBinding.setRoleRef(roleRef);

        roleBindingWrapper.create(clusterId, roleBinding);
    }

    @Override
    public void delete(String clusterId, String namespace, String name, Map<String, String> labels) {
        if (StringUtils.isNotEmpty(name)) {
            roleBindingWrapper.delete(clusterId, namespace, name);
        } else if (!CollectionUtils.isEmpty(labels)) {
            roleBindingWrapper.delete(clusterId, namespace, labels);
        }
    }
}
