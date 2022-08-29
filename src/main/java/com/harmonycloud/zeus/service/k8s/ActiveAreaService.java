package com.harmonycloud.zeus.service.k8s;

import com.harmonycloud.caas.common.model.ActiveAreaDto;
import com.harmonycloud.caas.common.model.ActivePoolDto;
import com.harmonycloud.caas.common.model.ClusterNodeResourceDto;
import com.harmonycloud.zeus.bean.BeanActiveArea;

import java.util.List;

/**
 * @author xutianhong
 * @Date 2022/5/10 2:45 下午
 */
public interface ActiveAreaService {

    /**
     * 划分双活资源池
     *
     * @param clusterId 集群id
     * @param activePoolDto 双活池业务对象
     */
    void dividePool(String clusterId, ActivePoolDto activePoolDto);

    /**
     * 获取双活资源池下节点列表
     *
     * @param clusterId 集群id
     * @return activePoolDto
     */
    ActivePoolDto getPoolNode(String clusterId);

    /**
     * 划分双活资源池
     *
     * @param clusterId 集群id
     * @param activeAreaDto 双活池业务对象
     */
    void divideActiveArea(String clusterId, ActiveAreaDto activeAreaDto);

    /**
     * 划分双活资源池
     *
     * @param clusterId 集群id
     * @param areaName 可用区名称
     * @param nodeName 节点名称
     */
    void removeActiveAreaNode(String clusterId, String areaName, String nodeName);

    /**
     * 获取可用区列表
     *
     * @param clusterId 集群id
     */
    List<ActiveAreaDto> list(String clusterId);

    /**
     * 划分双活资源池
     *
     * @param clusterId 集群id
     * @param areaName 可用区名称
     * @param aliasName 可用区别名
     */
    void update(String clusterId, String areaName, String aliasName);

    /**
     * 获取可用区资源情况
     *
     * @param clusterId 集群id
     * @param areaName 可用区名称
     * @return ActiveAreaDto
     */
    ActiveAreaDto getAreaResource(String clusterId, String areaName);

    /**
     * 获取可用区节点列表
     *
     * @param clusterId 集群id
     * @param areaName 可用区名称
     * @return ActiveAreaDto
     */
    List<ClusterNodeResourceDto> getAreaNode(String clusterId, String areaName);

    /**
     * 获取可用区详细信息
     * @param clusterId 集群id
     * @param areaName 可用区英文名
     * @return
     */
    BeanActiveArea get(String clusterId, String areaName);

    /**
     * 删除可用区初始化信息
     * @param clusterId
     */
    void delete(String clusterId);

    /**
     * 开启可用区
     * @param clusterId
     * @return
     */
    List<BeanActiveArea> initBeanActiveArea(String clusterId);
}
