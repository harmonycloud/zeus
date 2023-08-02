package com.middleware.zeus.service.k8s.impl;

import com.middleware.zeus.integration.cluster.ClusterRoleBindingWrapper;
import com.middleware.zeus.service.k8s.ClusterRoleBindingService;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.fabric8.kubernetes.api.model.rbac.ClusterRoleBinding;
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

import static com.middleware.zeus.common.constants.NameConstant.APP;
import static com.middleware.zeus.common.constants.NameConstant.ZEUS;

/**
 * @author xutianhong
 * @Date 2023/8/2 4:59 下午
 */
@Slf4j
@Service
public class ClusterRoleBindingServiceImpl implements ClusterRoleBindingService {

    private static final String USER = "User";

    @Autowired
    private ClusterRoleBindingWrapper clusterRoleBindingWrapper;


    @Override
    public void addUserClusterRoleBinding(String clusterId, String name, String username, String clusterRole) {
        ClusterRoleBinding clusterRoleBinding = clusterRoleBindingWrapper.get(clusterId, name);
        if (clusterRoleBinding == null && StringUtils.isNotEmpty(clusterRole)){
            this.create(clusterId, name, clusterRole);
            clusterRoleBinding = clusterRoleBindingWrapper.get(clusterId, name);
        }
        if (clusterRoleBinding == null){
            return;
        }

        List<Subject> subjectList = clusterRoleBinding.getSubjects();
        if (CollectionUtils.isEmpty(subjectList)){
            subjectList = new ArrayList<>();
        }
        // 若已存在当前用户 先移除
        subjectList.removeIf(subject -> USER.equals(subject.getKind()) && subject.getName().equals(username));

        // 添加用户
        Subject subject = new Subject();
        subject.setApiGroup("rbac.authorization.k8s.io");
        subject.setKind(USER);
        subject.setName(username);
        subjectList.add(subject);

        clusterRoleBinding.setSubjects(subjectList);

        clusterRoleBindingWrapper.update(clusterId, clusterRoleBinding);
    }

    @Override
    public void removeUserClusterRoleBinding(String clusterId, String name, String username) {
        ClusterRoleBinding clusterRoleBinding = clusterRoleBindingWrapper.get(clusterId, name);
        if (clusterRoleBinding == null || CollectionUtils.isEmpty(clusterRoleBinding.getSubjects())){
            return;
        }

        boolean remove = clusterRoleBinding.getSubjects().removeIf(subject -> USER.equals(subject.getKind()) && subject.getName().equals(username));
        if (!remove){
            return;
        }
        clusterRoleBindingWrapper.update(clusterId, clusterRoleBinding);
    }

    @Override
    public void create(String clusterId, String name, String clusterRole) {
        ClusterRoleBinding clusterRoleBinding = new ClusterRoleBinding();

        ObjectMeta meta = new ObjectMeta();
        meta.setName(name);

        Map<String, String> labels = new HashMap<>();
        labels.put(APP, ZEUS);
        meta.setLabels(labels);

        RoleRef roleRef = new RoleRef();
        roleRef.setApiGroup("rbac.authorization.k8s.io");
        roleRef.setKind("ClusterRole");
        roleRef.setName(clusterRole);

        clusterRoleBinding.setMetadata(meta);
        clusterRoleBinding.setRoleRef(roleRef);

        clusterRoleBindingWrapper.create(clusterId, clusterRoleBinding);
    }

}
