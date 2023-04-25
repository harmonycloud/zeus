package com.middleware.zeus.integration.cluster;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.CRDBasicInfo;
import com.middleware.zeus.util.K8sClient;
import io.fabric8.kubernetes.api.model.GenericKubernetesResource;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionList;
import io.fabric8.kubernetes.api.model.apiextensions.v1.CustomResourceDefinitionSpec;
import io.fabric8.kubernetes.client.dsl.base.CustomResourceDefinitionContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.*;

import static com.middleware.caas.common.constants.middleware.MiddlewareConstant.NAMESPACED;

/**
 * @author xutianhong
 * @Date 2021/8/12 1:49 下午
 */
@Slf4j
@Component
public class CustomResourceDefinitionWrapper {

    private static Map<String,CustomResourceDefinitionContext> crdContextMap = new HashMap<>();

    private static Map<String,String> namesMap = new HashMap<>();

    public List<CustomResourceDefinition> list(String clusterId) {
        CustomResourceDefinitionList crdList = K8sClient.getClient(clusterId).apiextensions().v1().customResourceDefinitions().list();

        if (CollectionUtils.isEmpty(crdList.getItems())) {
            return new ArrayList<>();
        }
        return crdList.getItems();
    }

    private List<CRDBasicInfo> listBasicInfo(String clusterId){
        List<CustomResourceDefinition> crdList = list(clusterId);
        List<CRDBasicInfo> crdBasicInfos = new ArrayList<>();
        for (CustomResourceDefinition crd : crdList) {
            CustomResourceDefinitionSpec spec = crd.getSpec();
            String group = spec.getGroup();
            String plural = spec.getNames().getPlural();
            String singular = spec.getNames().getSingular();
            String scope = spec.getScope();
            String version = spec.getVersions().get(0).getName();
            crdBasicInfos.add(new CRDBasicInfo(group, version, scope, singular, plural));
        }
        return crdBasicInfos;
    }

    private void initCrdContextMap(String clusterId){
        List<CRDBasicInfo> crdBasicInfos = listBasicInfo(clusterId);
        for (CRDBasicInfo crdBasicInfo : crdBasicInfos) {
            CustomResourceDefinitionContext crdContext = new CustomResourceDefinitionContext.Builder()
                    .withGroup(crdBasicInfo.getGroup())
                    .withVersion(crdBasicInfo.getVersion())
                    .withScope(NAMESPACED)
                    .withPlural(crdBasicInfo.getPlural())
                    .build();
            crdContextMap.put(crdBasicInfo.getPlural(), crdContext);
            namesMap.put(crdBasicInfo.getSingular(), crdBasicInfo.getPlural());
        }
    }

    public String getCRYaml(String clusterId, String namespace, String plural, String name) {
        CustomResourceDefinitionContext context = getContext(clusterId, plural);
        GenericKubernetesResource resource = K8sClient.getClient(clusterId).genericKubernetesResources(context).inNamespace(namespace).withName(name).get();
        Yaml yaml = new Yaml();
        return yaml.dumpAsMap(resource);
    }

    public String getCRPluralName(String clusterId, String singular) {
        String pluralName = namesMap.get(singular);
        if (StringUtils.isEmpty(pluralName)) {
            initCrdContextMap(clusterId);
            pluralName = namesMap.get(singular);
        }
        return pluralName;
    }

    public CustomResourceDefinitionContext getContext(String clusterId, String plural){
        CustomResourceDefinitionContext context = crdContextMap.get(plural);
        if (context == null) {
            initCrdContextMap(clusterId);
            context = crdContextMap.get(plural);
        }
        if(context == null){
            throw new BusinessException(ErrorMessage.UNSUPPORTED_YAML_TYPE);
        }
        return context;
    }

}
