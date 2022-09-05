package com.harmonycloud.zeus.service.k8s.impl;

import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.enums.IngressEnum;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.IngressComponentDto;
import com.harmonycloud.caas.common.model.middleware.IngressDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.MiddlewareValues;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.tool.date.DateUtils;
import com.harmonycloud.zeus.bean.BeanIngressComponents;
import com.harmonycloud.zeus.dao.BeanIngressComponentsMapper;
import com.harmonycloud.zeus.service.AbstractBaseService;
import com.harmonycloud.zeus.service.ingress.BaseIngressService;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.IngressComponentService;
import com.harmonycloud.zeus.service.k8s.IngressService;
import com.harmonycloud.zeus.service.k8s.PodService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.Yaml;

import java.util.*;
import java.util.stream.Collectors;

import static com.harmonycloud.caas.common.constants.CommonConstant.NUM_TWO;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_OPERATOR;

/**
 * @author xutianhong
 * @Date 2021/11/22 5:54 下午
 */
@Slf4j
@Service
public class IngressComponentServiceImpl extends AbstractBaseService implements IngressComponentService {

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private BeanIngressComponentsMapper beanIngressComponentsMapper;
    @Autowired
    private HelmChartService helmChartService;
    @Autowired
    private PodService podService;
    @Autowired
    private IngressService ingressService;

    @Override
    public void install(IngressComponentDto ingressComponentDto) {
        BaseIngressService service =
                getOperator(BaseIngressService.class, BaseIngressService.class, ingressComponentDto.getType());
        service.install(ingressComponentDto);
    }

    @Override
    public void integrate(IngressComponentDto ingressComponentDto) {
        // check exist
        QueryWrapper<BeanIngressComponents> wrapper = new QueryWrapper<BeanIngressComponents>().eq("ingress_class_name",
                ingressComponentDto.getIngressClassName()).eq("cluster_id", ingressComponentDto.getClusterId());
        BeanIngressComponents beanIngressComponents = beanIngressComponentsMapper.selectOne(wrapper);
        if (beanIngressComponents != null) {
            throw new BusinessException(ErrorMessage.INGRESS_CLASS_EXISTED);
        }
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
        // 更新数据库
        BeanUtils.copyProperties(ingressComponentDto, beanIngressComponents);
        beanIngressComponentsMapper.updateById(beanIngressComponents);
    }

    @Override
    public List<IngressComponentDto> list(String clusterId) {
        return list(clusterId, false);
    }

    @Override
    public List<IngressComponentDto> list(String clusterId,boolean filterUnavailable) {
        return list(clusterId, null, filterUnavailable);
    }

    @Override
    public List<IngressComponentDto> list(String clusterId, String type) {
        return list(clusterId, type, false);
    }

    @Override
    public List<IngressComponentDto> list(String clusterId, String type, boolean filterUnavailable) {
        // 获取数据库状态记录
        QueryWrapper<BeanIngressComponents> wrapper =
                new QueryWrapper<BeanIngressComponents>().eq("cluster_id", clusterId);
        if (StringUtils.isNotBlank(type)) {
            wrapper.eq("type", type);
        }
        List<BeanIngressComponents> ingressComponentsList = beanIngressComponentsMapper.selectList(wrapper);
        // 更新状态
        updateStatus(clusterId, ingressComponentsList);
        // 过滤掉不存在helm的ingress
        if (filterUnavailable) {
            ingressComponentsList = ingressComponentsList.stream().filter(ingress -> {
                JSONObject values = helmChartService.getInstalledValues(ingress.getName(), ingress.getNamespace(), clusterService.findById(ingress.getClusterId()));
                return values != null;
            }).collect(Collectors.toList());
        }

        // 封装数据
        return ingressComponentsList.stream().map(ingress -> {
            IngressComponentDto ic = new IngressComponentDto();
            BeanUtils.copyProperties(ingress, ic);
            //查询traefik起始端口
            if (IngressEnum.TRAEFIK.getName().equals(ic.getType())) {
                setTraefikStartPort(ic);
            }
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
            helmChartService.uninstall(cluster, MIDDLEWARE_OPERATOR, existIngress.getName());
        }
        // remove from mysql
        beanIngressComponentsMapper.deleteById(existIngress.getId());
    }

    @Override
    public void delete(String clusterId) {

    }

    @Override
    public List<String> vipList(String clusterId) {
        QueryWrapper<BeanIngressComponents> wrapper =
                new QueryWrapper<BeanIngressComponents>().eq("cluster_id", clusterId);
        List<BeanIngressComponents> beanIngressComponentsList = beanIngressComponentsMapper.selectList(wrapper);
        return beanIngressComponentsList.stream().map(BeanIngressComponents::getAddress).filter(StringUtils::isNotEmpty)
                .collect(Collectors.toList());
    }

    @Override
    public IngressComponentDto detail(String clusterId, String ingressClassName) {
        BeanIngressComponents ingressComponents = getAndCheckExists(clusterId, ingressClassName);
        BaseIngressService ingress =
                getOperator(BaseIngressService.class, BaseIngressService.class, ingressComponents.getType());
        return ingress.detail(ingressComponents);
    }

    @Override
    public List<PodInfo> pods(String clusterId, String ingressClassName) {
        BeanIngressComponents ingressComponents = getAndCheckExists(clusterId, ingressClassName);
        Map<String, String> labels = new HashMap<>();
        labels.put("app.kubernetes.io/instance", ingressComponents.getName());
        return podService.list(clusterId, ingressComponents.getNamespace(), labels);
    }

    @Override
    public List<IngressDTO> ports(String clusterId, String ingressClassName) {
        BeanIngressComponents ingressComponents = getAndCheckExists(clusterId, ingressClassName);
        List<IngressDTO> ingressDTOList = ingressService.listAllIngress(clusterId, null, null);
        return ingressDTOList.stream().filter(ingressDTO ->
                ingressComponents.getIngressClassName().equals(ingressDTO.getIngressClassName()) && ingressDTO.getNetworkModel() != 7).collect(Collectors.toList());
    }

    @Override
    public void restartPod(String clusterId, String ingressClassName, String podName) {
        BeanIngressComponents ingressComponents = getAndCheckExists(clusterId, ingressClassName);
        podService.restart(clusterId, ingressComponents.getNamespace(), podName);
    }

    @Override
    public String podYaml(String clusterId, String ingressClassName, String podName) {
        BeanIngressComponents beanIngressComponents = getAndCheckExists(clusterId, ingressClassName);
        return podService.yaml(clusterId, beanIngressComponents.getNamespace(), podName);
    }

    @Override
    public String values(String clusterId, String ingressClassName) {
        BeanIngressComponents ingressComponents = getAndCheckExists(clusterId, ingressClassName);
        Yaml yaml = new Yaml();
        return yaml.dumpAsMap(helmChartService.getInstalledValues(ingressComponents.getName(),
                ingressComponents.getNamespace(), clusterService.findById(ingressComponents.getClusterId())));
    }

    @Override
    public void upgrade(MiddlewareValues middlewareValues) {
        BeanIngressComponents ingressComponents = getAndCheckExists(middlewareValues.getClusterId(), middlewareValues.getName());
        middlewareValues.setNamespace(ingressComponents.getNamespace());
        BaseIngressService service =
                getOperator(BaseIngressService.class, BaseIngressService.class, ingressComponents.getType());
        service.upgrade(middlewareValues, ingressComponents.getName());
    }

    @Override
    public List<Integer> portCheck(String clusterId, String startPort) {
        List<Integer> conflictPortList = new ArrayList<>();
        Set<Integer> usedPortSet = ingressService.getUsedPortSet(clusterService.findById(clusterId));
        for (int i = 0; i < 100; i++) {
            int port = Integer.parseInt(startPort) + i;
            if (usedPortSet.contains(port)) {
                conflictPortList.add(port);
            }
        }
        return conflictPortList;
    }

    private void setTraefikStartPort(IngressComponentDto ingressComponentDto) {
        BeanIngressComponents ingressComponents = getAndCheckExists(ingressComponentDto.getClusterId(),
                ingressComponentDto.getIngressClassName());
        JSONObject values = helmChartService.getInstalledValues(ingressComponents.getName(),
                ingressComponents.getNamespace(), clusterService.findById(ingressComponents.getClusterId()));
        if (values != null) {
            ingressComponentDto.setStartPort(values.getString("startPort"));
            ingressComponentDto.setEndPort(values.getString("endPort"));
        }
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
        beanIngressComponents.setType(ingressComponentDto.getType());
        beanIngressComponentsMapper.insert(beanIngressComponents);
    }

    public void updateStatus(String clusterId, List<BeanIngressComponents> ingressComponentsList) {
        Map<String, String> nginxLabels = new HashMap<>(1);
        nginxLabels.put("app.kubernetes.io/name", "ingress-nginx");
        List<PodInfo> nginxPodInfoList = podService.list(clusterId, "middleware-operator", nginxLabels);
        Map<String, String> traefikLabels = new HashMap<>(1);
        traefikLabels.put("app.kubernetes.io/name", "traefik");
        List<PodInfo> traefikPodInfoList = podService.list(clusterId, "middleware-operator", traefikLabels);
        ingressComponentsList.forEach(ingress -> {
            if (ingress.getStatus() == 1) {
                return;
            }

            List<PodInfo> pods = null;
            if (IngressEnum.NGINX.getName().equals(ingress.getType())) {
                pods = nginxPodInfoList.stream().filter(podInfo -> {
                    String name = podInfo.getPodName();
                    return name.substring(0, name.lastIndexOf("-")).equals(ingress.getName() + "-controller");
                }).collect(Collectors.toList());
            } else if (IngressEnum.TRAEFIK.getName().equals(ingress.getType())) {
                pods = traefikPodInfoList.stream().filter(podInfo -> {
                    String name = podInfo.getPodName();
                    return name.substring(0, name.lastIndexOf("-")).equals(ingress.getName());
                }).collect(Collectors.toList());
            }

            // 默认正常
            int status = ingress.getStatus();
            if (CollectionUtils.isEmpty(pods)) {
                // 未安装
                status = 4;
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

    private BeanIngressComponents getAndCheckExists(String clusterId, String ingressClassName) {
        QueryWrapper<BeanIngressComponents> wrapper = new QueryWrapper<BeanIngressComponents>().eq("ingress_class_name",
                ingressClassName);
        wrapper.eq("cluster_id", clusterId);
        BeanIngressComponents beanIngressComponents = beanIngressComponentsMapper.selectOne(wrapper);
        if (beanIngressComponents == null) {
            throw new BusinessException(ErrorMessage.INGRESS_CLASS_NOT_EXISTED);
        }
        return beanIngressComponents;
    }

}
