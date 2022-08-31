package com.harmonycloud.zeus.service.ingress.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.IngressEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.AffinityDTO;
import com.harmonycloud.caas.common.model.ClusterComponentsDto;
import com.harmonycloud.caas.common.model.IngressComponentDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareValues;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.caas.common.util.ThreadPoolExecutorFactory;
import com.harmonycloud.tool.cmd.CmdExecUtil;
import com.harmonycloud.tool.cmd.HelmChartUtil;
import com.harmonycloud.tool.collection.JsonUtils;
import com.harmonycloud.tool.file.FileUtil;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.bean.BeanIngressComponents;
import com.harmonycloud.zeus.dao.BeanIngressComponentsMapper;
import com.harmonycloud.zeus.service.ingress.AbstractBaseOperator;
import com.harmonycloud.zeus.service.ingress.api.TraefikIngressService;
import com.harmonycloud.zeus.service.k8s.IngressService;
import com.harmonycloud.zeus.util.K8sConvert;
import io.fabric8.kubernetes.api.model.NodeAffinity;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.harmonycloud.caas.common.constants.CommonConstant.RESOURCE_ALREADY_EXISTED;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_OPERATOR;

/**
 * @author liyinlong
 * @since 2022/8/23 4:40 下午
 */
@Slf4j
@Service
@Operator(paramTypes4One = String.class)
public class TraefikIngressServiceImpl extends AbstractBaseOperator implements TraefikIngressService {

    @Value("${system.traefikPortNum:100}")
    private Integer traefikPortNum;
    @Autowired
    private IngressService ingressService;

    @Override
    public boolean support(String name) {
        return IngressEnum.TRAEFIK.getName().equals(name);
    }

    @Override
    public void install(IngressComponentDto ingressComponentDto) {
        MiddlewareClusterDTO cluster = clusterService.findById(ingressComponentDto.getClusterId());
        // check exist
        super.checkIfExists(ingressComponentDto);
        String repository = cluster.getRegistry().getRegistryAddress() + "/" + cluster.getRegistry().getChartRepo();
        // setValues
        String path = componentsPath + File.separator + "traefik";
        Yaml yaml = new Yaml();
        JSONObject values = yaml.loadAs(HelmChartUtil.getValueYaml(path), JSONObject.class);
        JSONObject image = values.getJSONObject("image");
        image.put("name", repository + "/traefik");

        JSONArray additionalArguments = values.getJSONArray("additionalArguments");
        List<String> portList = getPortList(cluster, ingressComponentDto.getStartPort(), ingressComponentDto.getIngressClassName(), traefikPortNum);
        additionalArguments.addAll(portList);
        values.put("startPort", portList.get(0).split(":")[1]);
        values.put("endPort", portList.get(portList.size() - 1).split(":")[1]);
        JSONObject ports = values.getJSONObject("ports");
        JSONObject web = ports.getJSONObject("web");
        JSONObject websecure = ports.getJSONObject("websecure");
        JSONObject traefik = ports.getJSONObject("traefik");
        JSONObject metrics = ports.getJSONObject("metrics");
        // 设置端口
        if (StringUtils.isNotEmpty(ingressComponentDto.getHttpPort())) {
            web.put("port", ingressComponentDto.getHttpPort());
            web.put("exposedPort", ingressComponentDto.getHttpPort());
        }
        if (StringUtils.isNotEmpty(ingressComponentDto.getHttpsPort())) {
            websecure.put("port", ingressComponentDto.getHttpsPort());
            websecure.put("exposedPort", ingressComponentDto.getHttpsPort());
        }
        if (StringUtils.isNotEmpty(ingressComponentDto.getDashboardPort())) {
            traefik.put("port", ingressComponentDto.getDashboardPort());
            traefik.put("exposedPort", ingressComponentDto.getDashboardPort());
        }
        if (StringUtils.isNotEmpty(ingressComponentDto.getMonitorPort())) {
            metrics.put("port", ingressComponentDto.getMonitorPort());
            metrics.put("exposedPort", ingressComponentDto.getMonitorPort());
        }
        JSONObject kubernetesIngress = values.getJSONObject("providers").getJSONObject("kubernetesIngress");
        kubernetesIngress.put("ingressClass", ingressComponentDto.getIngressClassName());
        values.put("fullnameOverride", ingressComponentDto.getIngressClassName());
        values.put("install", "true");
        // node affinity
        if (!CollectionUtils.isEmpty(ingressComponentDto.getNodeAffinity())) {
            // convert to k8s model
            JSONObject nodeAffinity = K8sConvert.convertNodeAffinity2Json(ingressComponentDto.getNodeAffinity());
            if (nodeAffinity != null) {
                JSONObject affinity = new JSONObject();
                affinity.put("nodeAffinity", nodeAffinity);
                values.put("affinity", affinity);
            }
        }
        // toleration
        if (!CollectionUtils.isEmpty(ingressComponentDto.getTolerations())) {
            JSONArray tolerationAry = K8sConvert.convertToleration2Json(ingressComponentDto.getTolerations());
            values.put("tolerations", tolerationAry);
            StringBuffer sbf = new StringBuffer();
            for (String toleration : ingressComponentDto.getTolerations()) {
                sbf.append(toleration).append(",");
            }
            values.put("tolerationAry", sbf.substring(0, sbf.length()));
        }
        // install
        helmChartService.installComponents(ingressComponentDto.getIngressClassName(), MIDDLEWARE_OPERATOR, path, values,
                values, cluster);
        // save to mysql
        ingressComponentDto.setNamespace(MIDDLEWARE_OPERATOR);
        ingressComponentDto.setAddress(ingressComponentDto.getAddress());
        ingressComponentDto.setType("traefik");
        super.insert(cluster.getId(), ingressComponentDto, 2);
        // 检查是否安装成功
        ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                Thread.sleep(55000);
                super.installSuccessCheck(cluster, ingressComponentDto);
            } catch (InterruptedException e) {
                log.error("更新组件安装中状态失败");
            }
        });
    }

    @Override
    public void integrate(IngressComponentDto ingressComponentDto) {

    }

    @Override
    public void update(IngressComponentDto ingressComponentDto) {

    }

    @Override
    public List<IngressComponentDto> list(String clusterId) {
        return null;
    }

    @Override
    public IngressComponentDto get(String clusterId, String ingressClassName) {
        return null;
    }

    @Override
    public void delete(String clusterId, String ingressClassName) {

    }

    @Override
    public void delete(String clusterId) {

    }

    @Override
    public List<String> vipList(String clusterId) {
        return null;
    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        return null;
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {

    }

    @Override
    protected void updateCluster(MiddlewareClusterDTO cluster) {

    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        return null;
    }

    @Override
    public IngressComponentDto detail(BeanIngressComponents ingressComponents) {
        JSONObject values = helmChartService.getInstalledValues(ingressComponents.getName(),
                ingressComponents.getNamespace(), clusterService.findById(ingressComponents.getClusterId()));
        IngressComponentDto ingressComponentDto = new IngressComponentDto();
        BeanUtils.copyProperties(ingressComponents, ingressComponentDto);
        if (values == null) {
            return ingressComponentDto;
        }
        JSONObject ports = values.getJSONObject("ports");
        ingressComponentDto.setHttpPort(ports.getJSONObject("web").getString("port"));
        ingressComponentDto.setHttpsPort(ports.getJSONObject("websecure").getString("port"));
        ingressComponentDto.setDashboardPort(ports.getJSONObject("traefik").getString("port"));
        ingressComponentDto.setMonitorPort(ports.getJSONObject("metrics").getString("port"));
        ingressComponentDto.setStartPort(values.getString("startPort"));
        ingressComponentDto.setEndPort(values.getString("endPort"));
        // node affinity
        if (JsonUtils.isJsonObject(values.getString("affinity"))) {
            JSONObject nodeAffinity = values.getJSONObject("affinity").getJSONObject("nodeAffinity");
            if (!CollectionUtils.isEmpty(nodeAffinity)) {
                List<AffinityDTO> dto = K8sConvert.convertNodeAffinity(
                        JSONObject.parseObject(nodeAffinity.toJSONString(), NodeAffinity.class), AffinityDTO.class);
                ingressComponentDto.setNodeAffinity(dto);
            }
        }
        // toleration
        if (values.getString("tolerationAry") != null) {
            String tolerationAry = values.getString("tolerationAry");
            ingressComponentDto.setTolerations(new ArrayList<>(Arrays.asList(tolerationAry.split(","))));
        }
        return ingressComponentDto;
    }

    @Override
    public void upgrade(MiddlewareValues middlewareValues, String ingressName) {
        super.upgrade(middlewareValues, ingressName, "traefik");
    }

    private List<String> execCmd(String cmd, Function<String, String> dealWithErrMsg) {
        List<String> res = new ArrayList<>();
        try {
            CmdExecUtil.execCmd(cmd, inputMsg -> {
                res.add(inputMsg);
                return inputMsg;
            }, dealWithErrMsg == null ? warningMsg() : dealWithErrMsg);
        } catch (Exception e) {
            if (StringUtils.isNotEmpty(e.getMessage()) && e.getMessage().contains(RESOURCE_ALREADY_EXISTED)) {
                log.error(e.getMessage());
                throw new BusinessException(ErrorMessage.RESOURCE_ALREADY_EXISTED);
            } else {
                throw e;
            }
        }
        return res;
    }

    private Function<String, String> warningMsg() {
        return errorMsg -> {
            if (errorMsg.startsWith("WARNING: ") || errorMsg.contains("warning: ")) {
                return errorMsg;
            }
            if (errorMsg.contains("OperatorConfiguration") || errorMsg.contains("operatorconfigurations")){
                return errorMsg;
            }
            if (errorMsg.contains("CustomResourceDefinition is deprecated") || errorMsg.contains("apiextensions.k8s.io/v1beta1")){
                return errorMsg;
            }
            throw new RuntimeException(errorMsg);
        };
    }

    private Function<String, String> notFoundMsg() {
        return errorMsg -> {
            if (errorMsg.startsWith("WARNING: ") || errorMsg.contains("warning: ") || errorMsg.endsWith("release: not found")) {
                return errorMsg;
            }
            throw new RuntimeException(errorMsg);
        };
    }

    private List<String> getPortList(MiddlewareClusterDTO cluster, String startPort, String ingressName, int num) {
        List<String> ports = new ArrayList<>();
        Set<Integer> usedPortSet = ingressService.getUsedPortSet(cluster);
        for (int i = 0, sum = 0; ; i++) {
            int port = Integer.parseInt(startPort) + i;
            if (!usedPortSet.contains(port)) {
                ports.add("--entrypoints." + ingressName + "-p" + port + ".Address=:" + port);
                sum++;
            }
            if (sum == num) {
                break;
            }
        }
        return ports;
    }

}
