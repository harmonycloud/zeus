package com.middleware.zeus.integration.cluster;

import com.middleware.caas.common.constants.PostgresqlConstant;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.CRDBasicInfo;
import com.middleware.zeus.util.K8sClient;
import com.middleware.zeus.util.YamlUtil;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceColumnDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionList;
import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinitionSpec;
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
        CustomResourceDefinitionList crdList = K8sClient.getClient(clusterId).customResourceDefinitions().list();
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
        Map<String, Object> map = K8sClient.getClient(clusterId).customResource(context).get(namespace, name);
        Map<String, Object> resMap = new LinkedHashMap<>();
        if (map.get("apiVersion") != null) {
            resMap.put("apiVersion", map.get("apiVersion"));
        }
        if (map.get("kind") != null) {
            resMap.put("kind", map.get("kind"));
        }
        if (map.get("spec") != null) {
            resMap.put("spec", map.get("spec"));
        }
        if (map.get("status") != null) {
            resMap.put("status", map.get("status"));
        }
        Yaml yaml = new Yaml();
        return yaml.dumpAsMap(resMap);
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
