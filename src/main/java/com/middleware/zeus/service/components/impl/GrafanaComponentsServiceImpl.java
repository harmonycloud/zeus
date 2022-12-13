package com.middleware.zeus.service.components.impl;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.enums.ComponentsEnum;
import com.middleware.caas.common.model.ClusterComponentsDto;
import com.middleware.caas.common.model.middleware.MiddlewareClusterDTO;
import com.middleware.caas.common.model.middleware.PodInfo;
import com.middleware.tool.cmd.HelmChartUtil;
import com.middleware.zeus.annotation.Operator;
import com.middleware.zeus.service.components.AbstractBaseOperator;
import com.middleware.zeus.service.components.api.GrafanaService;
import com.middleware.zeus.service.k8s.ClusterComponentService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;
import static com.middleware.caas.common.constants.CommonConstant.SIMPLE;

import java.io.File;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2021/10/29 4:11 下午
 */
@Service
@Operator(paramTypes4One = String.class)
public class GrafanaComponentsServiceImpl extends AbstractBaseOperator implements GrafanaService {

    public static final String GRAFANA = "grafana";
    @Autowired
    private ClusterComponentService clusterComponentService;

    @Override
    public boolean support(String name) {
        return ComponentsEnum.GRAFANA.getName().equals(name);
    }


    @Override
    public void deploy(MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        if (namespaceService.list(cluster.getId()).stream().noneMatch(ns -> "monitoring".equals(ns.getName()))){
            //创建分区
            namespaceService.save(cluster.getId(), "monitoring", null, null);
        }
        //获取仓库地址
        String repository = getRepository(cluster);
        //拼接参数
        Yaml yaml = new Yaml();
        String values = HelmChartUtil.getValueYaml(componentsPath + File.separator + "grafana");
        JSONObject jsonValues = new JSONObject();
        JSONObject image = new JSONObject();
        image.put("repository", repository + "/grafana");

        JSONObject sidecar = new JSONObject();
        JSONObject sidecarImage = new JSONObject();
        sidecarImage.put("repository", repository + "/k8s-sidecar");
        sidecar.put("image", sidecarImage);

        JSONObject persistence = new JSONObject();
        persistence.put("storageClassName", "local-path");

        jsonValues.put("image", image);
        jsonValues.put("sidecar", sidecar);
        jsonValues.put("persistence", persistence);

        ClusterComponentsDto componentsDto = clusterComponentService.get(cluster.getId(), "grafana");
        if (componentsDto != null && "https".equals(componentsDto.getProtocol())) {
            JSONObject ini = new JSONObject();
            JSONObject server = new JSONObject();
            server.put("protocol", "https");
            server.put("cert_file", "/etc/grafana/ssl/server.crt");
            server.put("cert_key", "/etc/grafana/ssl/server.key");
            ini.put("server", server);
            JSONObject readinessProbe = new JSONObject();
            JSONObject livenessProbe = new JSONObject();
            JSONObject httpGet = new JSONObject();
            httpGet.put("scheme", "HTTPS");
            readinessProbe.put("httpGet", httpGet);
            livenessProbe.put("httpGet", httpGet);
            jsonValues.put("grafana.ini", ini);
            jsonValues.put("readinessProbe", readinessProbe);
            jsonValues.put("livenessProbe", livenessProbe);
        }
        //高可用或单实例
        if (SIMPLE.equals(clusterComponentsDto.getType())) {
            jsonValues.put("replicas", 1);
        } else {
            jsonValues.put("replicas", 3);
        }
        //发布组件
        helmChartService.installComponents(ComponentsEnum.GRAFANA.getName(), "monitoring", componentsPath + File.separator + "grafana",
                yaml.loadAs(values, JSONObject.class), jsonValues, cluster);
        //更新middlewareCluster
        initAddress(clusterComponentsDto, cluster);
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        if (status != 1){
            helmChartService.uninstall(cluster, "monitoring",  ComponentsEnum.GRAFANA.getName());
        }
    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, ClusterComponentsDto clusterComponentsDto) {
        return null;
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {

    }

    @Override
    public void initAddress(ClusterComponentsDto clusterComponentsDto, MiddlewareClusterDTO cluster){
        if (StringUtils.isEmpty(clusterComponentsDto.getProtocol())){
            clusterComponentsDto.setProtocol("http");
        }
        if (StringUtils.isEmpty(clusterComponentsDto.getHost())){
            clusterComponentsDto.setHost(cluster.getHost());
        }
        if (StringUtils.isEmpty(clusterComponentsDto.getPort())){
            clusterComponentsDto.setPort("31900");
        }
    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        return podService.list(clusterId, "monitoring", ComponentsEnum.GRAFANA.getName());
    }

    @Override
    public void setStatus(ClusterComponentsDto clusterComponentsDto){
        if (StringUtils.isAnyEmpty(clusterComponentsDto.getProtocol(), clusterComponentsDto.getHost(),
                clusterComponentsDto.getPort())) {
            clusterComponentsDto.setStatus(7);
        }
    }


}
