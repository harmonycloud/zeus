package com.harmonycloud.zeus.service.components.impl;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.enums.ComponentsEnum;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterMonitor;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterMonitorInfo;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.tool.cmd.HelmChartUtil;
import com.harmonycloud.zeus.annotation.Operator;
import com.harmonycloud.zeus.service.components.AbstractBaseOperator;
import com.harmonycloud.zeus.service.components.api.GrafanaService;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;
import static com.harmonycloud.caas.common.constants.CommonConstant.SIMPLE;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author xutianhong
 * @Date 2021/10/29 4:11 下午
 */
@Service
@Operator(paramTypes4One = String.class)
public class GrafanaComponentsServiceImpl extends AbstractBaseOperator implements GrafanaService {

    public static final String GRAFANA = "grafana";

    @Override
    public boolean support(String name) {
        return ComponentsEnum.GRAFANA.getName().equals(name);
    }


    @Override
    public void deploy(MiddlewareClusterDTO cluster, String type) {
        if (namespaceService.list(cluster.getId()).stream().noneMatch(ns -> "monitoring".equals(ns.getName()))){
            //创建分区
            namespaceService.save(cluster.getId(), "monitoring", null);
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
        if (cluster.getMonitor() != null && cluster.getMonitor().getGrafana() != null
            && "https".equals(cluster.getMonitor().getGrafana().getProtocol())) {
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
        if (SIMPLE.equals(type)) {
            jsonValues.put("replicas", 1);
        } else {
            jsonValues.put("replicas", 3);
        }
        //发布组件
        helmChartService.upgradeInstall(ComponentsEnum.GRAFANA.getName(), "monitoring", componentsPath + File.separator + "grafana",
                yaml.loadAs(values, JSONObject.class), jsonValues, cluster);
        //更新middlewareCluster
        updateCluster(cluster);
    }

    @Override
    public void integrate(MiddlewareClusterDTO cluster) {
        MiddlewareClusterDTO existCluster = clusterService.findById(cluster.getId());
        if (existCluster.getMonitor() == null){
            existCluster.setMonitor(new MiddlewareClusterMonitor());
        }
        existCluster.getMonitor().setGrafana(cluster.getMonitor().getGrafana());
        clusterService.update(existCluster);
    }

    @Override
    public void delete(MiddlewareClusterDTO cluster, Integer status) {
        if (status != 1){
            helmChartService.uninstall(cluster, "monitoring",  ComponentsEnum.GRAFANA.getName());
        }
        if (cluster.getMonitor().getGrafana() != null){
            cluster.getMonitor().setGrafana(null);
        }
        clusterService.update(cluster);
    }

    @Override
    protected String getValues(String repository, MiddlewareClusterDTO cluster, String type) {
        return null;
    }

    @Override
    protected void install(String setValues, MiddlewareClusterDTO cluster) {

    }

    @Override
    protected void updateCluster(MiddlewareClusterDTO cluster) {
        MiddlewareClusterMonitorInfo grafana = new MiddlewareClusterMonitorInfo();
        grafana.setProtocol(cluster.getMonitor().getGrafana().getProtocol()).setPort("31900")
                .setHost(cluster.getHost());
        if (cluster.getMonitor() == null){
            cluster.setMonitor(new MiddlewareClusterMonitor());
        }
        cluster.getMonitor().setGrafana(grafana);
        clusterService.update(cluster);
    }

    @Override
    protected List<PodInfo> getPodInfoList(String clusterId) {
        return podService.list(clusterId, "monitoring", ComponentsEnum.GRAFANA.getName());
    }


}
