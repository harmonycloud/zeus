package com.harmonycloud.zeus.service.k8s.impl;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.util.ThreadPoolExecutorFactory;
import com.harmonycloud.tool.date.DateUtils;
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

import static com.harmonycloud.caas.common.constants.CommonConstant.NUM_TWO;

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
        // todo check exist
        /*if (!CollectionUtils.isEmpty(cluster.getIngressList())) {
            if (cluster.getIngressList().stream()
                .anyMatch(ingress -> ingress.getIngressClassName().equals(ingressComponentDto.getIngressClassName()))) {
                throw new BusinessException(ErrorMessage.INGRESS_CLASS_EXISTED);
            }
        }*/
        String repository = cluster.getRegistry().getRegistryAddress() + "/" + cluster.getRegistry().getChartRepo();
        // setValues
        String setValues = "image.ingressRepository=" + repository +
                ",image.backendRepository=" + repository +
                ",image.keepalivedRepository=" + repository +
                ",httpPort=" + ingressComponentDto.getHttpPort() +
                ",httpsPort=" + ingressComponentDto.getHttpsPort() +
                ",healthzPort=" + ingressComponentDto.getHealthzPort() +
                ",defaultServerPort=" + ingressComponentDto.getDefaultServerPort() +
                ",ingressClass=" + ingressComponentDto.getIngressClassName() +
                ",fullnameOverride=" + ingressComponentDto.getIngressClassName() +
                ",install=true";
        // install
        helmChartService.installComponents(ingressComponentDto.getIngressClassName(), "middleware-operator", setValues,
            componentsPath + File.separator + "ingress-nginx/charts/ingress-nginx", cluster);
        // update cluster
        /*MiddlewareClusterIngress ingress = new MiddlewareClusterIngress().setAddress(cluster.getHost())
            .setIngressClassName(ingressComponentDto.getIngressClassName());
        MiddlewareClusterIngress.IngressConfig config = new MiddlewareClusterIngress.IngressConfig();
        config.setEnabled(true).setNamespace("middleware-operator")
            .setConfigMapName(ingressComponentDto.getIngressClassName() + "-system-expose-nginx-config-tcp");
        ingress.setTcp(config);
        if (CollectionUtils.isEmpty(cluster.getIngressList())) {
            cluster.setIngressList(new ArrayList<>());
        }
        cluster.getIngressList().add(ingress);
        clusterService.update(cluster);*/
        // save to mysql
        ingressComponentDto.setNamespace("middleware-operator");
        ingressComponentDto.setConfigMapName(ingressComponentDto.getIngressClassName() + "-system-expose-nginx-config-tcp");
        ingressComponentDto.setAddress(cluster.getHost());
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
        /*MiddlewareClusterDTO existCluster = clusterService.findById(ingressComponentDto.getClusterId());
        if (CollectionUtils.isEmpty(existCluster.getIngressList())) {
            existCluster.setIngressList(new ArrayList<>());
        }
        if (existCluster.getIngressList().stream()
            .anyMatch(ingress -> ingress.getIngressClassName().equals(ingressComponentDto.getIngressClassName()))) {
            throw new BusinessException(ErrorMessage.INGRESS_CLASS_EXISTED);
        }
        MiddlewareClusterIngress ingress = new MiddlewareClusterIngress();
        ingress.setIngressClassName(ingressComponentDto.getIngressClassName())
            .setAddress(ingressComponentDto.getAddress()).setTcp(new MiddlewareClusterIngress.IngressConfig());
        ingress.getTcp().setNamespace(ingressComponentDto.getNamespace())
            .setConfigMapName(ingressComponentDto.getConfigMapName()).setEnabled(true);

        existCluster.getIngressList().add(ingress);
        clusterService.update(existCluster);*/
        // save to mysql
        insert(ingressComponentDto.getClusterId(), ingressComponentDto, 1);
    }

    @Override
    public void update(IngressComponentDto ingressComponentDto) {
        MiddlewareClusterDTO cluster = clusterService.findById(ingressComponentDto.getClusterId());
        QueryWrapper<BeanIngressComponents> wrapper = new QueryWrapper<BeanIngressComponents>().eq("id", ingressComponentDto.getId());
        BeanIngressComponents beanIngressComponents = beanIngressComponentsMapper.selectOne(wrapper);
        // 校验存在
        if (beanIngressComponents == null) {
            throw new BusinessException(ErrorMessage.INGRESS_CLASS_NOT_EXISTED);
        }
        // 更新集群信息
        /*cluster.getIngressList().forEach(ingress -> {
            if (ingress.getIngressClassName().equals(beanIngressComponents.getIngressClassName())) {
                ingress.setAddress(ingressComponentDto.getAddress())
                    .setIngressClassName(ingressComponentDto.getIngressClassName());
                ingress.getTcp().setConfigMapName(ingressComponentDto.getConfigMapName())
                    .setNamespace(ingressComponentDto.getNamespace());
            }
        });
        clusterService.update(cluster);*/
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

        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        /*if (CollectionUtils.isEmpty(cluster.getIngressList())){
            return new ArrayList<>();
        }*/
        //数据同步
        /*if (cluster.getIngressList().size() > ingressComponentsList.size()) {
            synchronization(cluster, ingressComponentsList);
        }*/
        // 更新状态
        updateStatus(clusterId, ingressComponentsList);
        /*Map<String, BeanIngressComponents> ingressComponentsMap = ingressComponentsList.stream().collect(Collectors
            .toMap(BeanIngressComponents::getIngressClassName, beanIngressComponents -> beanIngressComponents));*/
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
        // update cluster
        /*MiddlewareClusterIngress exist =
            cluster.getIngressList().stream().filter(ingress -> ingress.getIngressClassName().equals(ingressClassName))
                .collect(Collectors.toList()).get(0);
        cluster.getIngressList().remove(exist);
        clusterService.update(cluster);*/
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

    /*public void synchronization(MiddlewareClusterDTO cluster, List<BeanIngressComponents> ingressComponentsList){
        //过滤获取未在数据库记录的ingress
        List<
            MiddlewareClusterIngress> ingressList =
                cluster.getIngressList().stream()
                    .filter(ingress -> ingressComponentsList.stream()
                        .noneMatch(ic -> ic.getName().equals(ingress.getIngressClassName())))
                    .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(ingressList)){
            ingressList.forEach(ingress -> {
                BeanIngressComponents bean = new BeanIngressComponents();
                bean.setClusterId(cluster.getId());
                bean.setName(ingress.getIngressClassName());
                bean.setStatus(1);
                beanIngressComponentsMapper.insert(bean);
                ingressComponentsList.add(bean);
            });
        }
    }*/

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
