package com.middleware.zeus.operator;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.model.middleware.*;
import io.fabric8.kubernetes.api.model.ConfigMap;

import java.util.List;
import java.util.Map;

/**
 * @author dengyulong
 * @date 2021/03/23
 * 中间件通用接口
 */
public interface BaseOperator {

    boolean support(Middleware middleware);

    /**
     * 查询中间件列表
     *
     * @param middleware 中间件信息
     * @param keyword    关键词，模糊搜索
     * @return
     */
    List<Middleware> list(Middleware middleware, String keyword);

    /**
     * 查询中间件详情
     *
     * @param middleware 中间件信息
     * @return
     */
    Middleware detail(Middleware middleware);

    /**
     * 查询中间件切换信息
     *
     * @param middleware 中间件信息
     * @return
     */
    SwitchInfo getAutoSwitch(Middleware middleware);

    /**
     * 根据helm chart转换
     *
     * @param middleware 中间件信息
     * @param cluster    集群信息
     * @return
     */
    Middleware convertByHelmChart(Middleware middleware, MiddlewareClusterDTO cluster);

    /**
     * 创建中间件
     *
     * @param middleware 中间件信息
     * @param cluster    集群
     */
    void create(Middleware middleware, MiddlewareClusterDTO cluster);

    /**
     * 创建校验
     *
     * @param middleware 中间件信息
     * @param cluster    集群
     */
    void createPreCheck(Middleware middleware, MiddlewareClusterDTO cluster);

    /**
     * 修改中间件
     *
     * @param middleware 中间件信息
     * @param cluster    集群
     */
    void update(Middleware middleware, MiddlewareClusterDTO cluster);

    /**
     * 修改校验
     *
     * @param middleware 中间件信息
     * @param cluster    集群
     */
    void updatePreCheck(Middleware middleware, MiddlewareClusterDTO cluster);

    /**
     * 删除中间件
     *
     * @param middleware 中间件信息
     */
    void delete(Middleware middleware);

    /**
     * 删除中间件相关存储
     *
     * @param middleware 中间件信息
     */
    void deleteStorage(Middleware middleware);

    /**
     * 手动切换中间件
     *
     * @param middleware 中间件信息
     */
    void switchMiddleware(Middleware middleware);

    /**
     * 指定从节点手动切换主从
     *
     * @param middleware 中间件信息
     * @param slaveName 从节点名称
     */
    void switchMiddleware(Middleware middleware, String slaveName);


    /**
     * pvc扩缩容
     *
     * @param middleware 中间件信息
     * @return
     */
    void updateStorage(Middleware middleware);

    /**
     * 获取自定义配置列表
     *
     * @param configMap 配置文件
     * @return List<CustomConfig>
     */
    List<String> getConfigmapDataList(ConfigMap configMap);

    /**
     * 计算中间件cpu申请使用量
     *
     * @param values 配置文件
     */
    Double calculateCpuRequest(JSONObject values);

    /**
     * 获取自定义配置列表
     *
     * @param configMap 配置文件
     * @return List<CustomConfig>
     */
    @Deprecated
    Map<String, String> configMap2Data(ConfigMap configMap);

    /**
     * 修改data
     *
     * @param customConfig 自定义参数
     * @param data
     */
    @Deprecated
    void editConfigMapData(CustomConfig customConfig, List<String> data);

    /**
     * 更新自定义配置
     *
     * @param configMap 配置文件
     * @param data      配置内容
     * @return void
     */
    @Deprecated
    void updateConfigData(ConfigMap configMap, List<String> data);

    /**
     * 查询中间件hostnetwork访问地址
     */
    List<IngressDTO> listHostNetworkAddress(String clusterId, String namespace, String middlewareName, String type);

}
