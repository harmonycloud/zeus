package com.middleware.zeus.service.k8s.impl;

import com.middleware.zeus.integration.cluster.ClusterRoleWrapper;
import com.middleware.zeus.service.k8s.ClusterRoleService;
import com.middleware.zeus.util.file.FileUtil;
import io.fabric8.kubernetes.api.model.rbac.ClusterRole;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.File;

/**
 * @author xutianhong
 * @Date 2023/8/3 3:48 下午
 */
@Slf4j
@Service
public class ClusterRoleServiceImpl implements ClusterRoleService {

    @Value("${system.clusterRole.path:/usr/local/zeus-pv/clusterRole}")
    private String path;
    
    @Autowired
    private ClusterRoleWrapper clusterRoleWrapper;
    
    @Override
    public void initClusterRole(String clusterId) {
        File filePath = new File(path);
        if (!filePath.exists() || !filePath.isDirectory()) {
            return;
        }
        File[] fileList = filePath.listFiles();
        if (fileList == null) {
            return;
        }
        Yaml yaml = new Yaml();
        for (File file : fileList) {
            String path = file.getAbsolutePath();
            if (!path.endsWith("yaml")){
                continue;
            }
            try {
                // 读取clusterRole yaml文件
                String clusterRoleYaml = FileUtil.readFile(file.getAbsolutePath());
                // 转换为clusterRole对象
                ClusterRole clusterRole = yaml.loadAs(clusterRoleYaml, ClusterRole.class);
                // 创建clusterRole
                clusterRoleWrapper.create(clusterId, clusterRole);
            } catch (Exception e) {
                log.error("集群{} 初始化集群clusterRole资源对象失败", clusterId, e);
            }
        }
    }
}
