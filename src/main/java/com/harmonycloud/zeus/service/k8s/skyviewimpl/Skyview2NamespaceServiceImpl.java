package com.harmonycloud.zeus.service.k8s.skyviewimpl;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.base.CaasResult;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.impl.NamespaceServiceImpl;
import com.harmonycloud.zeus.service.user.ProjectService;
import com.harmonycloud.zeus.skyviewservice.Skyview2NamespaceServiceClient;
import com.harmonycloud.zeus.util.ZeusCurrentUser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liyinlong
 * @since 2022/6/21 2:19 下午
 */
@Slf4j
@Service
@ConditionalOnProperty(value="system.usercenter",havingValue = "skyview2")
public class Skyview2NamespaceServiceImpl extends NamespaceServiceImpl {

    @Autowired
    private Skyview2NamespaceServiceClient  namespaceServiceClient;
    @Autowired
    private ProjectService projectService;
    @Autowired
    private ClusterService clusterService;

    private List<Namespace> listClusterNamespaces(String clusterId){
        CaasResult<JSONArray> namespaceResult = namespaceServiceClient.clusterNamespaces(ZeusCurrentUser.getCaasToken(),
                clusterService.convertToSkyviewClusterId(clusterId));
        List<Namespace> namespaces = new ArrayList<>();
        if(namespaceResult.getData() != null && !namespaceResult.getData().isEmpty()){
            for (Object ns : namespaceResult.getData()) {
                JSONObject  jsonNamespace = (JSONObject) ns;
                Namespace namespace = new Namespace();
                namespace.setName(jsonNamespace.getString("name"));
                namespace.setAliasName(jsonNamespace.getString("aliasName"));
                namespace.setCreateTime(jsonNamespace.getDate("createTime"));
                namespace.setPhase(jsonNamespace.getString("phase"));
                namespace.setRegistered(true);
                namespaces.add(namespace);
            }
        }
        return namespaces;
    }

    @Override
    public List<Namespace> list(String clusterId, boolean all, boolean withQuota, boolean withMiddleware,
                                String keyword, String projectId) {
        List<Namespace> namespaceList;
        if (StringUtils.isNotEmpty(projectId)) {
            namespaceList = projectService.getNamespace(projectId).stream().
                    filter(namespace -> clusterId.equals(namespace.getClusterId())).
                    collect(Collectors.toList());
        } else {
            namespaceList = listClusterNamespaces(clusterId);
        }

        if (withQuota) {
            super.listNamespaceWithQuota(namespaceList, clusterId);
        }
        if (withMiddleware) {
            super.listNamespaceWithMiddleware(namespaceList, clusterId);
        }
        if (StringUtils.isNotEmpty(keyword)) {
            return namespaceList.stream().filter(namespace -> namespace.getName().contains(keyword)).collect(Collectors.toList());
        }
        return namespaceList;
    }

    @Override
    public List<Namespace> list(String clusterId) {
        return listClusterNamespaces(clusterId);
    }

}
