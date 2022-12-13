package com.middleware.zeus.service.registry.impl;

import static com.middleware.caas.common.constants.CommonConstant.RESOURCE_ALREADY_EXISTED;
import static com.middleware.caas.common.constants.CommonConstant.SIMPLE;
import static com.middleware.caas.common.constants.registry.HelmChartConstant.*;
import static com.middleware.caas.common.constants.MirrorImageConstant.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.middleware.caas.common.model.middleware.*;
import com.middleware.zeus.bean.BeanImageRepository;
import com.middleware.zeus.integration.registry.HelmChartWrapper;
import com.middleware.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.middleware.zeus.integration.registry.bean.harbor.V1HelmChartVersion;
import com.middleware.zeus.service.k8s.NamespaceService;
import com.middleware.zeus.service.middleware.ImageRepositoryService;
import com.middleware.zeus.service.registry.AbstractRegistryService;
import com.middleware.zeus.service.registry.HelmChartService;
import com.middleware.zeus.util.YamlUtil;
import com.middleware.zeus.service.k8s.ClusterCertService;
import com.middleware.zeus.service.k8s.ClusterService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;
import org.yaml.snakeyaml.Yaml;

import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.enums.registry.RegistryType;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.exception.CaasRuntimeException;
import com.middleware.caas.common.model.registry.HelmChartFile;
import com.middleware.zeus.bean.BeanMiddlewareInfo;
import com.middleware.zeus.service.middleware.MiddlewareInfoService;
import com.middleware.zeus.service.middleware.MiddlewareService;
import com.middleware.tool.cmd.CmdExecUtil;
import com.middleware.tool.cmd.HelmChartUtil;
import com.middleware.tool.file.FileUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * @author dengyulong
 * @date 2021/03/29
 */
@Slf4j
@Service
public class HelmChartServiceImpl extends AbstractRegistryService implements HelmChartService {

    /**
     * helm chart的上传子目录
     */
    private static final String SUB_DIR = "/helmcharts/";

    @Autowired
    private HelmChartWrapper helmChartWrapper;
    @Autowired
    private ClusterCertService clusterCertService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private MiddlewareService middlewareService;
    @Autowired
    private MiddlewareInfoService middlewareInfoService;
    @Autowired
    private NamespaceService namespaceService;
    @Autowired
    private ImageRepositoryService imageRepositoryService;

    @Value("${system.upload.path:/usr/local/zeus-pv/upload}")
    private String uploadPath;
    @Value("${k8s.component.middleware:/usr/local/zeus-pv/middleware}")
    private String middlewarePath;

    @Deprecated
    @Override
    public List<V1HelmChartVersion> listHelmChartVersions(Registry registry, String chartName) {
        return helmChartWrapper.listHelmChartVersions(registry, chartName);
    }

    @Deprecated
    @Override
    public HelmChartFile getHelmChart(String clusterId, String namespace, String name, String type) {
        Middleware middleware = middlewareService.detail(clusterId, namespace, name, type);
        if (StringUtils.isEmpty(middleware.getChartVersion())){
            BeanMiddlewareInfo beanMiddlewareInfo = middlewareInfoService.list(true).stream()
                .filter(info -> info.getChartName().equals(type)).collect(Collectors.toList()).get(0);
            return getHelmChartFromMysql(type, beanMiddlewareInfo.getChartVersion());
        }
        return getHelmChartFromMysql(type, middleware.getChartVersion());
    }

    @Override
    public HelmChartFile getHelmChartFromMysql(String chartName, String chartVersion) {
        BeanMiddlewareInfo mwInfo = middlewareInfoService.get(chartName, chartVersion);
        if (mwInfo.getChart() == null || mwInfo.getChart().length == 0) {
            throw new BusinessException(ErrorMessage.NOT_FOUND);
        }
        //创建upload路径
        File upload = new File(uploadPath);
        if (!upload.exists() && !upload.mkdirs()) {
            throw new BusinessException(ErrorMessage.CREATE_TEMPORARY_FILE_ERROR);
        }
        File file = new File(uploadPath + File.separator + chartName + "-" + chartVersion + ".tgz");
        if (!file.exists()) {
            InputStream in = new ByteArrayInputStream(mwInfo.getChart());
            FileOutputStream fileOutputStream = null;
            try {
                fileOutputStream = new FileOutputStream(file);
                byte[] buf = new byte[mwInfo.getChart().length];
                int len;
                while ((len = in.read(buf)) != -1) {
                    fileOutputStream.write(buf, 0, len);
                }
                fileOutputStream.flush();
            } catch (Exception e) {
                throw new BusinessException(ErrorMessage.HELM_CHART_WRITE_ERROR);
            } finally {
                try {
                    in.close();
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                } catch (IOException io) {
                }
            }
        }
        try {
            return this.getHelmChartFromFile(chartName, chartVersion, file);
        } finally {
            file.delete();
        }
    }

    @Deprecated
    @Override
    public File downloadHelmChart(Registry registry, String chartName, String chartVersion) {
        if (!RegistryType.isSelfType(registry.getType())) {
            throw new BusinessException(ErrorMessage.REGISTRY_TYPE_NOT_SUPPORT_INTERFACE);
        }
        return helmChartWrapper.downloadHelmChart(registry, chartName, chartVersion);
    }

    @Override
    public HelmChartFile getHelmChartFromFile(String chartName, String chartVersion, File file) {
        File tarFileDir = null;
        String helmChartPath = null;
        // 如果没有指定chatName，则认为是手动上传的文件
        boolean isManual = StringUtils.isBlank(chartName);
        if (isManual) {
            helmChartPath = file.getParent();
        } else {
            helmChartPath = getHelmChartFilePath(chartName, chartVersion);
        }
        tarFileDir = new File(helmChartPath);

        if (null != tarFileDir && !tarFileDir.exists() && !tarFileDir.mkdirs()) {
            throw new BusinessException(ErrorMessage.CREATE_TEMPORARY_FILE_ERROR);
        }
        List<String> results = CmdExecUtil.runCmd(false, "cd", file.getParent(), "&&", "tar", "-zxvf",
            file.getAbsolutePath(), "-C", helmChartPath);
        if (CollectionUtils.isEmpty(results)) {
            log.error("解压chart文件错误");
            throw new BusinessException(ErrorMessage.HELM_CHART_UNZIP_ERROR);
        }
        log.info("chart 文件内容:{}", JSONObject.toJSONString(results));
        String[] resultsArr;
        if (results.get(0).startsWith("./") && results.size()>1) {
            resultsArr = results.get(1).split(" ");
        } else {
            resultsArr = results.get(0).split(" ");
        }
        String tarFileName = resultsArr[resultsArr.length - 1].split(File.separator)[0];
        String tarFilePath = helmChartPath + File.separator + tarFileName;
        // 从下载的包获取参数文件，描述，资源yaml
        String valueYaml = HelmChartUtil.getValueYaml(tarFilePath);
        HelmChartFile chartFile = convertChart(tarFilePath);
        return chartFile.setValueYaml(valueYaml).setFileIndex("1").setTarFileName(tarFileName);
    }
    
    public HelmChartFile convertChart(String tarFilePath) {
        Map<String, Object> infoMap = HelmChartUtil.getInfoMap(tarFilePath);
        String description = infoMap.get("description") == null ? null : infoMap.get("description").toString();
        String iconPath = infoMap.get(ICON) == null ? null : infoMap.get(ICON).toString();
        String appVersion = infoMap.get("appVersion") == null ? null : infoMap.get("appVersion").toString();
        String official = "";
        String type = "";
        String version = "";
        Object object = infoMap.getOrDefault("annotations", "");
        String compatibleVersions = null;
        if (!ObjectUtils.isEmpty(object)) {
            JSONObject annotations = JSONObject.parseObject(JSONObject.toJSONString(object));
            official = annotations.getOrDefault("owner", "other").toString();
            type = annotations.getOrDefault("type", "").toString();
            version = annotations.getOrDefault("version", "").toString();
            compatibleVersions = annotations.get("compatibleVersions") == null ? null : annotations.get("compatibleVersions").toString();
        }
        List<Map<String, String>> dependencies = infoMap.containsKey("dependencies")
            ? (List<Map<String, String>>)infoMap.get("dependencies") : new ArrayList<>();
        String chartName = infoMap.get("name") == null ? null : infoMap.get("name").toString();
        String chartVersion = infoMap.get("version") == null ? null : infoMap.get("version").toString();

        Map<String, String> yamlFileMap = HelmChartUtil.getYamlFileMap(tarFilePath);
        yamlFileMap.putAll(HelmChartUtil.getParameters(tarFilePath));
        if (!CollectionUtils.isEmpty(dependencies)){
            String operatorName = dependencies.get(0).get("alias");
            yamlFileMap.putAll(HelmChartUtil.getCrds(tarFilePath, operatorName));
        }
        yamlFileMap.put(CHART_YAML_NAME, JSONObject.toJSONString(infoMap));

        return new HelmChartFile().setDescription(description).setIconPath(iconPath).setType(type)
            .setAppVersion(appVersion).setOfficial(official).setYamlFileMap(yamlFileMap).setVersion(version)
            .setDependency(CollectionUtils.isEmpty(dependencies) ? new HashMap<>() : dependencies.get(0))
            .setChartName(chartName).setChartVersion(chartVersion).setCompatibleVersions(compatibleVersions);
    }
    

    @Override
    public void coverYamlFile(HelmChartFile helmChart) {
        String unzipTarFilePath = getHelmChartFilePath(helmChart.getChartName(), helmChart.getChartVersion())
            + File.separator + helmChart.getTarFileName();
        //特殊处理logstash
        if("logstash".equals(helmChart.getChartName())){
            helmChart.setValueYaml(helmChart.getValueYaml().replace("|-", ""));
        }
        try {
            // 覆盖写入values.yaml
            FileUtil.writeToLocal(unzipTarFilePath, VALUES_YAML_NAME,
                helmChart.getValueYaml().replace("\\n", "\n").replace("\\\"", "\""));
        } catch (IOException e) {
            log.error("写出values.yaml文件异常：chart包{}:{}", helmChart.getChartName(), helmChart.getChartVersion(), e);
            throw new BusinessException(ErrorMessage.HELM_CHART_WRITE_ERROR);
        }
    }

    @Override
    public List<HelmListInfo> listHelm(String namespace, String name, MiddlewareClusterDTO cluster) {
        String cmd = "helm list --kube-apiserver " + cluster.getAddress() + " --kubeconfig "
            + clusterCertService.getKubeConfigFilePath(cluster.getId())
            + (StringUtils.isBlank(namespace) ? " -A" : " -n " + namespace);
        List<String> res = execCmd(cmd, null);
        if (CollectionUtils.isEmpty(res)) {
            return new ArrayList<>(0);
        }
        List<HelmListInfo> list = new ArrayList<>(res.size() - 1);
        boolean filterByName = StringUtils.isNotBlank(name);
        for (int i = 1; i < res.size(); i++) {
            // NAME NAMESPACE REVISION UPDATED STATUS CHART APP VERSION
            // mysql default 1 2021-03-31 15:53:57.044997 +0800 CST deployed mysql-0.1.0 5.7.21
            String[] infos = res.get(i).split("\\s+");
            if ((filterByName && !infos[0].equals(name)) || infos.length < 10) {
                continue;
            }
            HelmListInfo helm = new HelmListInfo();
            helm.setName(infos[0]);
            helm.setNamespace(infos[1]);
            helm.setRevision(infos[2]);
            helm.setUpdateTime(infos[3] + infos[4] + infos[5] + infos[6]);
            helm.setStatus(infos[7]);
            helm.setChart(infos[8]);
            helm.setAppVersion(infos[9]);
            list.add(helm);
        }
        return list;
    }

    @Override
    public JSONObject getInstalledValues(Middleware middleware, MiddlewareClusterDTO cluster) {
        return getInstalledValues(middleware.getName(), middleware.getNamespace(), cluster);
    }

    @Override
    public JSONObject getInstalledValues(String name, String namespace, MiddlewareClusterDTO cluster) {
        String yamlStr = loadYamlAsStr(name, namespace, cluster);
        if (StringUtils.isEmpty(yamlStr)){
            return null;
        }
        Yaml yaml = new Yaml();
        return yaml.loadAs(yamlStr, JSONObject.class);
    }

    @Override
    public JSONObject getInstalledValuesAsNormalJson(String name, String namespace, MiddlewareClusterDTO cluster) {
        return YamlUtil.convertYamlAsNormalJsonObject(loadYamlAsStr(name, namespace, cluster));
    }

    @Override
    public String packageChart(String unzipFileName, String chartName, String chartVersion) {
        String tarFileDir = getHelmChartFilePath(chartName, chartVersion);
        // 打包文件，返回信息如：Successfully packaged chart and saved it to: /xxx/xx/mysql-0.1.0.tgz，取/最后一段为包名
        String packageCmd =
            String.format("helm package %s -d %s", tarFileDir + File.separator + unzipFileName, tarFileDir);
        List<String> packageRes = execCmd(packageCmd, null);
        String tgzFileName = packageRes.get(0).substring(packageRes.get(0).lastIndexOf("/") + 1);
        return tarFileDir + File.separator + tgzFileName;
    }

    @Override
    public void install(Middleware middleware, String tgzFilePath, MiddlewareClusterDTO cluster) {
        install(middleware.getName(), middleware.getNamespace(), middleware.getChartName(),
            middleware.getChartVersion(), tgzFilePath, cluster);
    }

    @Override
    public void install(String name, String namespace, String chartName, String chartVersion, String tgzFilePath,
                        MiddlewareClusterDTO cluster) {
        String tarFileDir = getHelmChartFilePath(chartName, chartVersion);
        String cmd = String.format("helm install %s %s --kube-apiserver %s --kubeconfig %s -n %s", name, tgzFilePath,
            cluster.getAddress(), clusterCertService.getKubeConfigFilePath(cluster.getId()), namespace);

        // 先dry-run发布下，避免包不正确
        execCmd(cmd + " --dry-run", null);
        // 正式发布
        execCmd(cmd, null);
        // 把当前目录删除
        FileUtil.deleteFile(tarFileDir);
    }

    @Override
    public QuestionYaml getQuestionYaml(HelmChartFile helmChartFile) {
        try {
            Yaml yaml = new Yaml();
            JSONObject question = yaml.loadAs(HelmChartUtil
                .getQuestionYaml(getHelmChartFilePath(helmChartFile.getChartName(), helmChartFile.getChartVersion())
                    + File.separator + helmChartFile.getTarFileName() + File.separator + MANIFESTS),
                JSONObject.class);
            return convertType(JSONObject.parseObject(JSONObject.toJSONString(question), QuestionYaml.class));
        } catch (Exception e) {
            log.error("中间件{} 获取question.yaml失败", helmChartFile.getChartName() + ":" + helmChartFile.getChartVersion());
            throw new CaasRuntimeException(ErrorMessage.CREATE_DYNAMIC_FORM_FAILED);
        }
    }

    public QuestionYaml convertType(QuestionYaml questionYaml) {
        List<Question> questions = questionYaml.getQuestions();
        questions.stream().forEach(question -> {
            if (MIRROR_IMAGE.equals(question.getLabel())) {
                question.setType("mirrorImage");
            }
        });
        return questionYaml;
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
            if (errorMsg.contains("PodSecurityPolicy is deprecated")){
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

    @Override
    public void upgrade(Middleware middleware, String updateValues, MiddlewareClusterDTO cluster) {
        // helm upgrade
        if (StringUtils.isBlank(updateValues)) {
            return;
        }
        String chartName = middleware.getChartName();
        String chartVersion = middleware.getChartVersion();

        // 先获取chart文件
        HelmChartFile helmChart = getHelmChartFromMysql(chartName, chartVersion);

        JSONObject values = getInstalledValues(middleware, cluster);
        Yaml yaml = new Yaml();
        String valuesYaml = yaml.dumpAsMap(values);
        String tempValuesYamlDir = getTempValuesYamlDir();
        String tempValuesYamlName = chartName + "-" + chartVersion + "-" + System.currentTimeMillis() + ".yaml";
        try {
            FileUtil.writeToLocal(tempValuesYamlDir, tempValuesYamlName, valuesYaml);
        } catch (IOException e) {
            log.error("写出values.yaml文件异常：chart包{}:{}", helmChart.getChartName(), helmChart.getChartVersion(), e);
            throw new BusinessException(ErrorMessage.HELM_CHART_WRITE_ERROR);
        }

        String tgzFilePath = getTgzFilePath(chartName, chartVersion);
        File tgzFile = new File(tgzFilePath);
        if (!tgzFile.exists()) {
            // helm package
            tgzFilePath = packageChart(helmChart.getTarFileName(), chartName, chartVersion);
        }
        String tempValuesYamlPath = tempValuesYamlDir + File.separator + tempValuesYamlName;
        String cmd = String.format("helm upgrade %s %s --values %s --set %s -n %s --kube-apiserver %s --kubeconfig %s ",
            middleware.getName(), tgzFilePath, tempValuesYamlPath, updateValues, middleware.getNamespace(),
            cluster.getAddress(), clusterCertService.getKubeConfigFilePath(cluster.getId()));
        try {
            execCmd(cmd, null);
        } finally {
            // 删除文件
            FileUtil.deleteFile(tempValuesYamlPath, getHelmChartFilePath(chartName, chartVersion));
        }
    }

    @Override
    public void upgrade(Middleware middleware, JSONObject values, JSONObject newValues,
        MiddlewareClusterDTO cluster) {

        String chartName = middleware.getChartName();
        String chartVersion = middleware.getChartVersion();
        String tempValuesYamlDir = getTempValuesYamlDir();

        // 先获取chart文件
        HelmChartFile helmChart = getHelmChartFromMysql(chartName, chartVersion);

        String tempValuesYamlName =
            chartName + "-" + chartVersion + "-" + "temp" + "-" + System.currentTimeMillis() + ".yaml";
        String targetValuesYamlName =
            chartName + "-" + chartVersion + "-" + "target" + "-" + System.currentTimeMillis() + ".yaml";

        Yaml yaml = new Yaml();
        String tempValuesYaml = yaml.dumpAsMap(values);
        String targetValuesYaml = yaml.dumpAsMap(newValues);
        try {
            FileUtil.writeToLocal(tempValuesYamlDir, tempValuesYamlName, tempValuesYaml);
            FileUtil.writeToLocal(tempValuesYamlDir, targetValuesYamlName, targetValuesYaml);
        } catch (IOException e) {
            log.error("写出values.yaml文件异常：chart包{}:{}", helmChart.getChartName(), helmChart.getChartVersion(), e);
            throw new BusinessException(ErrorMessage.HELM_CHART_WRITE_ERROR);
        }

        String helmPath = getHelmChartFilePath(chartName, chartVersion) + File.separator + chartName;
        String tempValuesYamlPath = tempValuesYamlDir + File.separator + tempValuesYamlName;
        String targetValuesYamlPath = tempValuesYamlDir + File.separator + targetValuesYamlName;

        String cmd = String.format("helm upgrade --install %s %s -f %s -f %s -n %s --kube-apiserver %s --kubeconfig %s ",
            middleware.getName(), helmPath, tempValuesYamlPath, targetValuesYamlPath, middleware.getNamespace(),
            cluster.getAddress(), clusterCertService.getKubeConfigFilePath(cluster.getId()));
        try {
            execCmd(cmd, null);
        } finally {
            // 删除文件
            FileUtil.deleteFile(tempValuesYamlPath, targetValuesYamlPath,
                getHelmChartFilePath(chartName, chartVersion));
        }
    }

    @Override
    public void installComponents(String name, String namespace, String path, JSONObject values, JSONObject newValues,
                                  MiddlewareClusterDTO cluster) {
        Yaml yaml = new Yaml();
        String tempValuesYaml = yaml.dumpAsMap(values);
        String targetValuesYaml = yaml.dumpAsMap(newValues);

        String tempValuesYamlName = "temp" + "-" + System.currentTimeMillis() + ".yaml";
        String targetValuesYamlName = "target" + "-" + System.currentTimeMillis() + ".yaml";
        try {
            FileUtil.writeToLocal(uploadPath, tempValuesYamlName, tempValuesYaml);
            FileUtil.writeToLocal(uploadPath, targetValuesYamlName, targetValuesYaml);
        } catch (IOException e) {
            log.error("写出values.yaml文件异常：chart包", e);
            throw new BusinessException(ErrorMessage.HELM_CHART_WRITE_ERROR);
        }

        String tempValuesYamlPath = uploadPath + File.separator + tempValuesYamlName;
        String targetValuesYamlPath = uploadPath + File.separator + targetValuesYamlName;

        String cmd =
            String.format("helm upgrade --install %s %s -f %s -f %s -n %s --kube-apiserver %s --kubeconfig %s ", name,
                path, tempValuesYamlPath, targetValuesYamlPath, namespace, cluster.getAddress(),
                clusterCertService.getKubeConfigFilePath(cluster.getId()));
        try {
            execCmd(cmd, null);
        } finally {
            // 删除文件
            FileUtil.deleteFile(tempValuesYamlPath, targetValuesYamlPath);
        }
    }

    @Override
    public void installComponents(String name, String namespace, String setValues, String chartUrl,
                                  MiddlewareClusterDTO cluster) {
        String cmd = String.format("helm upgrade --install %s %s --set %s -n %s --kube-apiserver %s --kubeconfig %s ",
            name, chartUrl, setValues, namespace, cluster.getAddress(),
            clusterCertService.getKubeConfigFilePath(cluster.getId()));
        execCmd(cmd, null);
    }

    @Override
    public void uninstall(Middleware middleware, MiddlewareClusterDTO cluster) {
        // query first, if not exists then we need not do anything
        List<HelmListInfo> helms = listHelm(middleware.getNamespace(), middleware.getName(), cluster);
        if (CollectionUtils.isEmpty(helms)) {
            return;
        }

        // delete helm
        String cmd = String.format("helm uninstall %s -n %s --kube-apiserver %s --kubeconfig %s", middleware.getName(),
            middleware.getNamespace(), cluster.getAddress(), clusterCertService.getKubeConfigFilePath(cluster.getId()));
        execCmd(cmd, null);
    }

    @Override
    public void uninstall(MiddlewareClusterDTO cluster, String namespace, String operatorName) {
        List<HelmListInfo> helms = listHelm(namespace, operatorName, cluster);
        if (CollectionUtils.isEmpty(helms)) {
            return;
        }
        // delete helm
        String cmd = String.format("helm uninstall %s -n %s --kube-apiserver %s --kubeconfig %s", operatorName,
                namespace, cluster.getAddress(), clusterCertService.getKubeConfigFilePath(cluster.getId()));
        execCmd(cmd, null);
    }

    @Override
    public void editOperatorChart(String clusterId, String operatorChartPath, String type) {
        Yaml yaml = new Yaml();
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        BeanImageRepository registry = imageRepositoryService.getClusterDefaultRegistry(cluster.getId());
        JSONObject values = yaml.loadAs(HelmChartUtil.getValueYaml(operatorChartPath), JSONObject.class);
        values.getJSONObject("image").put("repository", registry.getAddress());
        //高可用或单实例
        if (SIMPLE.equals(type)) {
            values.put("replicaCount",1);
        } else {
            values.put("replicaCount",3);
        }
        try {
            // 覆盖写入values.yaml
            FileUtil.writeToLocal(operatorChartPath, VALUES_YAML_NAME,
                yaml.dumpAsMap(values).replace("\\n", "\n").replace("\\\"", "\""));
        } catch (IOException e) {
            throw new BusinessException(ErrorMessage.HELM_CHART_WRITE_ERROR);
        }
    }

    @Override
    public void createOperator(String tempDirPath, String clusterId, HelmChartFile helmChartFile, String type) {
        try {
            MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
            // 创建middleware-operator
            namespaceService.createMiddlewareOperator(clusterId);
            // 检验operator是否已创建
            List<HelmListInfo> helmListInfoList =
                this.listHelm("", helmChartFile.getDependency().get("alias"), cluster);
            if (!CollectionUtils.isEmpty(helmListInfoList)) {
                return;
            }
            // 获取地址
            String path = tempDirPath + File.separator + helmChartFile.getTarFileName()
                + helmChartFile.getDependency().get("repository").substring(8);
            File operatorDir = new File(path);
            if (operatorDir.exists()) {
                // 创建operator
                this.editOperatorChart(clusterId, path, type);
                this.install(helmChartFile.getDependency().get("alias"), "middleware-operator",
                    helmChartFile.getChartName(), helmChartFile.getChartVersion(), path, cluster);
            } else {
                log.error("中间件{} operator包不存在", helmChartFile.getChartName());
                throw new BusinessException(ErrorMessage.NOT_EXIST);
            }
        } catch (Exception e) {
            log.error("集群{} 中间件{} 创建operator失败", clusterId, helmChartFile.getChartName());
            throw new BusinessException(ErrorMessage.CREATE_MIDDLEWARE_OPERATOR_FAILED);
        }
    }

    @Override
    public String getChartVersion(JSONObject values, String type) {
        if (StringUtils.isNotEmpty(values.getString("chart-version"))) {
            return values.getString("chart-version");
        } else {
            List<BeanMiddlewareInfo> beanMiddlewareInfoList = middlewareInfoService.list(true).stream()
                .filter(info -> info.getChartName().equals(type)).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(beanMiddlewareInfoList)) {
                return beanMiddlewareInfoList.get(0).getChartVersion();
            }
        }
        return null;
    }

    private String getUploadPath() {
        return uploadPath + SUB_DIR;
    }

    private String getHelmChartFilePath(String chartName, String chartVersion) {
        return getUploadPath() + chartName + File.separator + chartVersion;
    }
    
    private String getTempValuesYamlDir() {
        return getUploadPath() + "values";
    }
    
    private String getTgzFilePath(String chartName, String chartVersion) {
        return getHelmChartFilePath(chartName, chartVersion) + File.separator + chartName + "-" + chartVersion + ".tgz";
    }

    private String getLocalTgzPath(String chartName, String chartVersion){
        return middlewarePath + File.separator + chartName + "-" + chartVersion + ".tgz";
    }

    /**
     * 获取中间件values.yaml文件内容，并解析为字符串
     * @param name 中间件名称
     * @param namespace 分区
     * @param cluster 集群
     * @return
     */
    private String loadYamlAsStr(String name, String namespace, MiddlewareClusterDTO cluster) {
        String cmd = String.format("helm get values %s -n %s -a --kube-apiserver %s --kubeconfig %s", name, namespace,
                cluster.getAddress(), clusterCertService.getKubeConfigFilePath(cluster.getId()));
        List<String> values = execCmd(cmd, notFoundMsg());
        if (CollectionUtils.isEmpty(values)) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        // 第0行是COMPUTED VALUES:，直接跳过
        for (int i = 1; i < values.size(); i++) {
            sb.append(values.get(i)).append("\n");
        }
        return sb.toString();
    }

}
