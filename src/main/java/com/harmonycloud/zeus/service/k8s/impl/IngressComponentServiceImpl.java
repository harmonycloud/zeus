package com.harmonycloud.zeus.service.k8s.impl;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.util.ThreadPoolExecutorFactory;
import com.harmonycloud.tool.cmd.HelmChartUtil;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.zeus.util.K8sConvert;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.IngressComponentDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterIngress;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.zeus.bean.BeanIngressComponents;
import com.harmonycloud.zeus.dao.BeanIngressComponentsMapper;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.IngressComponentService;
import com.harmonycloud.zeus.service.k8s.PodService;
import com.harmonycloud.zeus.service.registry.HelmChartService;

import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import static com.harmonycloud.caas.common.constants.CommonConstant.NUM_TWO;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_OPERATOR;

/**
 * @author xutianhong
 * @Date 2021/11/22 5:54 下午
 */
@Service
@Slf4j
public class IngressComponentServiceImpl implements IngressComponentService {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private BeanIngressComponentsMapper beanIngressComponentsMapper;
    @Autowired
    private PodService podService;
    @Value("${k8s.component.components:/usr/local/zeus-pv/components}")
    private String componentsPath;

    @Override
    public void install(IngressComponentDto ingressComponentDto) {
        MiddlewareClusterDTO cluster = clusterService.findById(ingressComponentDto.getClusterId());
        // check exist
        QueryWrapper<BeanIngressComponents> wrapper = new QueryWrapper<BeanIngressComponents>().eq("ingress_class_name",
            ingressComponentDto.getIngressClassName());
        BeanIngressComponents beanIngressComponents = beanIngressComponentsMapper.selectOne(wrapper);
        if (beanIngressComponents != null) {
            throw new BusinessException(ErrorMessage.INGRESS_CLASS_EXISTED);
        }
        String repository = cluster.getRegistry().getRegistryAddress() + "/" + cluster.getRegistry().getChartRepo();
        // setValues
        String path = componentsPath + File.separator + "ingress-nginx/charts/ingress-nginx";
        Yaml yaml = new Yaml();
        JSONObject values = yaml.loadAs(HelmChartUtil.getValueYaml(path), JSONObject.class);
        JSONObject image = values.getJSONObject("image");
        image.put("ingressRepository", repository);
        image.put("backendRepository", repository);
        image.put("keepalivedRepository", repository);

        // 设置端口
        if (StringUtils.isNotEmpty(ingressComponentDto.getHttpPort())) {
            values.put("httpPort", ingressComponentDto.getHttpPort());
        }
        if (StringUtils.isNotEmpty(ingressComponentDto.getHttpsPort())) {
            values.put("httpsPort", ingressComponentDto.getHttpsPort());
        }
        if (StringUtils.isNotEmpty(ingressComponentDto.getHealthzPort())) {
            values.put("healthzPort", ingressComponentDto.getHealthzPort());
        }
        if (StringUtils.isNotEmpty(ingressComponentDto.getDefaultServerPort())) {
            values.put("defaultServerPort", ingressComponentDto.getDefaultServerPort());
        }

        values.put("ingressClass", ingressComponentDto.getIngressClassName());
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
            JSONArray toleration = K8sConvert.convertToleration2Json(ingressComponentDto.getTolerations());
            values.put("tolerations", toleration);
        }
        // install
        helmChartService.installComponents(ingressComponentDto.getIngressClassName(), MIDDLEWARE_OPERATOR, path, values,
            values, cluster);
        // save to mysql
        ingressComponentDto.setNamespace(MIDDLEWARE_OPERATOR);
        ingressComponentDto
            .setConfigMapName(ingressComponentDto.getIngressClassName() + "-system-expose-nginx-config-tcp");
        ingressComponentDto.setAddress(ingressComponentDto.getAddress());
        insert(cluster.getId(), ingressComponentDto, 2);
        // 检查是否安装成功
        ThreadPoolExecutorFactory.executor.execute(() -> {
            try {
                Thread.sleep(55000);
                installSuccessCheck(cluster, ingressComponentDto);
            } catch (InterruptedException e) {
                log.error("更新组件安装中状态失败");
            }
        });
    }

    @Override
    public void integrate(IngressComponentDto ingressComponentDto) {
        // save to mysql
        insert(ingressComponentDto.getClusterId(), ingressComponentDto, 1);
    }

    @Override
    public void update(IngressComponentDto ingressComponentDto) {
        QueryWrapper<BeanIngressComponents> wrapper = new QueryWrapper<BeanIngressComponents>().eq("id", ingressComponentDto.getId());
        BeanIngressComponents beanIngressComponents = beanIngressComponentsMapper.selectOne(wrapper);
        // 校验存在
        if (beanIngressComponents == null) {
            throw new BusinessException(ErrorMessage.INGRESS_CLASS_NOT_EXISTED);
        }
        // todo 更新数据库
        BeanUtils.copyProperties(ingressComponentDto, beanIngressComponents);
        beanIngressComponentsMapper.updateById(beanIngressComponents);
    }

    @Override
    public List<IngressComponentDto> list(String clusterId) {
        // 获取数据库状态记录
        QueryWrapper<BeanIngressComponents> wrapper =
            new QueryWrapper<BeanIngressComponents>().eq("cluster_id", clusterId);
        List<BeanIngressComponents> ingressComponentsList = beanIngressComponentsMapper.selectList(wrapper);
        // 更新状态
        updateStatus(clusterId, ingressComponentsList);
        // 封装数据
        return ingressComponentsList.stream().map(ingress -> {
            IngressComponentDto ic = new IngressComponentDto();
            BeanUtils.copyProperties(ingress, ic);
            if (ic.getStatus() == NUM_TWO) {
                ic.setSeconds(DateUtils.getIntervalDays(new Date(), ic.getCreateTime()));
            }
            return ic;
        }).collect(Collectors.toList());
    }

    @Override
    public IngressComponentDto get(String clusterId, String ingressClassName) {
        QueryWrapper<BeanIngressComponents> wrapper = new QueryWrapper<BeanIngressComponents>()
            .eq("cluster_id", clusterId).eq("ingress_class_name", ingressClassName);
        BeanIngressComponents beanIngressComponents = beanIngressComponentsMapper.selectOne(wrapper);
        if (beanIngressComponents == null) {
            return null;
        }
        IngressComponentDto ingressComponentDto = new IngressComponentDto();
        BeanUtils.copyProperties(beanIngressComponents, ingressComponentDto);
        return ingressComponentDto;
    }

    @Override
    public void delete(String clusterId, String ingressClassName) {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        QueryWrapper<BeanIngressComponents> wrapper =
            new QueryWrapper<BeanIngressComponents>().eq("cluster_id", clusterId).eq("ingress_class_name", ingressClassName);
        BeanIngressComponents existIngress = beanIngressComponentsMapper.selectOne(wrapper);
        if (existIngress.getStatus() != 1) {
            helmChartService.uninstall(cluster, "middleware-operator", existIngress.getName());
        }
        // remove from mysql
        beanIngressComponentsMapper.deleteById(existIngress.getId());
    }

    @Override
    public void delete(String clusterId) {
        QueryWrapper<BeanIngressComponents> wrapper = new QueryWrapper<BeanIngressComponents>().eq("cluster_id", clusterId);
        beanIngressComponentsMapper.delete(wrapper);
    }

    /**
     * 封装dao对象
     */
    public void insert(String clusterId, IngressComponentDto ingressComponentDto, Integer status) {
        BeanIngressComponents beanIngressComponents = new BeanIngressComponents();
        beanIngressComponents.setClusterId(clusterId);
        BeanUtils.copyProperties(ingressComponentDto, beanIngressComponents);
        beanIngressComponents.setName(ingressComponentDto.getIngressClassName());
        beanIngressComponents.setStatus(status);
        beanIngressComponents.setCreateTime(new Date());
        beanIngressComponentsMapper.insert(beanIngressComponents);
    }

    public void updateStatus(String clusterId, List<BeanIngressComponents> ingressComponentsList) {
        Map<String, String> labels = new HashMap<>(1);
        labels.put("app.kubernetes.io/name", "ingress-nginx");
        List<PodInfo> podInfoList = podService.list(clusterId, "middleware-operator", labels);
        ingressComponentsList.forEach(ingress -> {
            if (ingress.getStatus() == 1) {
                return;
            }
            List<PodInfo> pods = podInfoList.stream().filter(podInfo -> {
                String name = podInfo.getPodName();
                return name.substring(0, name.lastIndexOf("-")).equals(ingress.getName() + "-controller");
            }).collect(Collectors.toList());
            // 默认正常
            int status = ingress.getStatus();
            if (CollectionUtils.isEmpty(pods)) {
                // 未安装
                beanIngressComponentsMapper.deleteById(ingress.getId());
                return;
            } else if (pods.stream()
                .allMatch(pod -> "Running".equals(pod.getStatus()) || "Completed".equals(pod.getStatus()))) {
                status = 3;
            } else if (ingress.getStatus() != 5 && ingress.getStatus() != 2 && ingress.getStatus() != 6) {
                // 非卸载或安装中或安装异常 则为运行异常
                status = 4;
            }
            ingress.setStatus(status);
            beanIngressComponentsMapper.updateById(ingress);
        });
    }

    public void installSuccessCheck(MiddlewareClusterDTO cluster, IngressComponentDto ingressComponentDto) {
        QueryWrapper<BeanIngressComponents> wrapper = new QueryWrapper<BeanIngressComponents>()
            .eq("cluster_id", cluster.getId()).eq("ingress_class_name", ingressComponentDto.getIngressClassName());
        BeanIngressComponents ingressComponents = beanIngressComponentsMapper.selectOne(wrapper);
        if (ingressComponents.getStatus() == 2) {
            ingressComponents.setStatus(6);
            beanIngressComponentsMapper.updateById(ingressComponents);
        }
    }

}
