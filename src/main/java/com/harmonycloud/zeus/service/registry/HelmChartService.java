package com.harmonycloud.zeus.service.registry;

import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.caas.common.model.middleware.QuestionYaml;
import com.harmonycloud.caas.common.model.middleware.Registry;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.harmonycloud.zeus.integration.registry.bean.harbor.V1HelmChartVersion;

import java.io.File;
import java.util.List;

/**
 * @author dengyulong
 * @date 2021/03/29
 */
public interface HelmChartService {

    /**
     * 查询chart包的所有版本
     *
     * @param registry  制品服务信息
     * @param chartName chart名称
     * @return
     */
    List<V1HelmChartVersion> listHelmChartVersions(Registry registry, String chartName);

    /**
     *从本地获取chart包并解析文件
     *
     * @param chartName    chart名称
     * @param chartVersion chart版本
     * @return
     */
    HelmChartFile getHelmChartFromLocal(String chartName, String chartVersion);

    /**
     * 下载chart包并解析文件
     *
     * @param registry     制品服务信息
     * @param chartName    chart名称
     * @param chartVersion chart版本
     * @return
     */
    HelmChartFile getHelmChartFromRegistry(Registry registry, String chartName, String chartVersion);

    /**
     * 下载chart包并解析文件
     *
     * @param clusterId 集群
     * @param namespace 分区
     * @param type 类型
     * @param name 名称
     * @return
     */
    HelmChartFile getHelmChartFromRegistry(String clusterId, String namespace, String name, String type);

    /**
     * 下载helm chart包
     *
     * @param registry     制品服务信息
     * @param chartName    chart名称
     * @param chartVersion chart版本
     * @return
     */
    File downloadHelmChart(Registry registry, String chartName, String chartVersion);

    /**
     * 从下载本地的chart包中解析文件
     *
     * @param chartName    chart名称
     * @param chartVersion chart版本
     * @param file         chart包
     * @return
     */
    HelmChartFile getHelmChartFromFile(String chartName, String chartVersion, File file);

    /**
     * 把新的yaml内容覆盖掉本地文件
     *
     * @param helmChart helm chart信息
     */
    void coverYamlFile(HelmChartFile helmChart);

    /**
     * 把新的yaml内容覆盖掉本地文件(template内文件)
     *
     * @param helmChart helm chart信息
     * @param fileName 文件名
     */
    void coverTemplateFile(HelmChartFile helmChart, String fileName);

    /**
     * 查询helm chart列表
     *
     * @param namespace 命名空间，选填
     * @param name
     * @param cluster   集群信息
     * @return
     */
    List<HelmListInfo> listHelm(String namespace, String name, MiddlewareClusterDTO cluster);

    /**
     * 获取已发布helm chart的values
     *
     * @param middleware 中间件信息
     * @param cluster    集群信息
     * @return
     */
    JSONObject getInstalledValues(Middleware middleware, MiddlewareClusterDTO cluster);

    /**
     * 获取已发布helm chart的values
     *
     * @param name      helm发布实例的名称
     * @param namespace 命名空间
     * @param cluster   集群信息
     * @return
     */
    JSONObject getInstalledValues(String name, String namespace, MiddlewareClusterDTO cluster);

    /**
     * 打包chart文件
     *
     * @param unzipFileName 解压的文件名
     * @param chartName     chart名
     * @param chartVersion  chart版本
     * @return
     */
    String packageChart(String unzipFileName, String chartName, String chartVersion);

    /**
     * 发布helm chart包
     * @param middleware    中间件信息
     * @param tgzFilePath   tgz文件的绝对路径（包含文件名）
     * @param cluster       集群信息
     */
    void install(Middleware middleware, String tgzFilePath, MiddlewareClusterDTO cluster);

    /**
     * 发布helm chart包
     *
     * @param name          helm发布的实例名称
     * @param namespace     命名空间
     * @param chartName     chart包名称
     * @param chartVersion  chart包版本
     * @param tgzFilePath   tgz文件的绝对路径（包含文件名）
     * @param cluster       集群信息
     */
    void install(String name, String namespace, String chartName, String chartVersion, String tgzFilePath, MiddlewareClusterDTO cluster);

    /**
     * 发布helm chart包
     *
     * @param name          helm发布的实例名称
     * @param namespace     命名空间
     * @param tgzFilePath   tgz文件的绝对路径（包含文件名）
     * @param cluster       集群信息
     */
    void install(String name, String namespace, String tgzFilePath, MiddlewareClusterDTO cluster);

    /**
     * 更新已发布的helm chart
     *
     * @param middleware   中间件信息
     * @param updateValues 修改的values，多个值用英文逗号分隔，如aliasName=n1,test=true
     * @param cluster      集群信息
     */
    void upgrade(Middleware middleware, String updateValues, MiddlewareClusterDTO cluster);

    /**
     * 更新自定义配置参数
     *
     * @param middleware   中间件信息
     * @param values       values
     * @param newValues    新values
     * @param cluster      集群信息
     */
    void upgrade(Middleware middleware, JSONObject values, JSONObject newValues, MiddlewareClusterDTO cluster);

    /**
     * 更新/发布 chart
     *
     * @param name         helm发布的实例名称
     * @param namespace    命名空间
     * @param setValues    设置的值
     * @param chartName    chart名称
     * @param chartVersion chart版本
     * @param cluster      集群信息
     */
    void upgradeInstall(String name, String namespace, String setValues, String chartName, String chartVersion,
                        MiddlewareClusterDTO cluster);

    /**
     * 更新/发布 chart
     *
     * @param name      helm发布的实例名称
     * @param namespace 命名空间
     * @param setValues 设置的值
     * @param chartUrl  远端chart文件地址
     * @param cluster   集群信息
     */
    void upgradeInstall(String name, String namespace, String setValues, String chartUrl, MiddlewareClusterDTO cluster);

    /**
     * 卸载已发布的helm chart
     *
     * @param middleware 中间件信息
     * @param cluster    集群信息
     */
    void uninstall(Middleware middleware, MiddlewareClusterDTO cluster);

    /**
     * 卸载已发布的helm chart
     *
     * @param cluster    集群信息
     * @param operatorName 中间件信息
     * @param namespace 分区
     */
    void uninstall(MiddlewareClusterDTO cluster, String namespace, String operatorName);

    /**
     * 获取helm中的question.yaml
     *
     * @param helmChartFile helm包
     * @return QuestionYaml
     */
    QuestionYaml getQuestionYaml(HelmChartFile helmChartFile);

    /**
     * 修改operator chart包中values的内容
     *
     * @param clusterId 集群id
     * @param operatorChartPath chart包位置
     */
    void editOperatorChart(String clusterId, String operatorChartPath);

    /**
     * 从mysql中取出helm chart
     *
     * @param chartName chart名称
     * @param chartVersion chart版本
     */
    HelmChartFile getHelmChartFromMysql(String chartName, String chartVersion) throws Exception;

    /**
     * 创建operator
     *
     * @param chartPath helm chart包路径
     * @param clusterId 集群id
     * @param helmChartFile chart包对象
     */
    void createOperator(String chartPath, String clusterId, HelmChartFile helmChartFile);

}
