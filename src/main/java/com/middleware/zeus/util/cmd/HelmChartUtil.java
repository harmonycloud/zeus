package com.middleware.zeus.util.cmd;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.alibaba.fastjson.JSONObject;
import com.middleware.zeus.util.collection.MapUtils;
import com.middleware.zeus.util.file.FileUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import static com.middleware.zeus.common.constants.CommonConstant.*;

/**
 * @author dengyulong
 * @date 2020/12/24
 */
public class HelmChartUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelmChartUtil.class);

    /**
     * 获取chart包的values.yaml
     *
     * @param chartFilePath chart文件路径
     * @return
     */
    public static String getValueYaml(String chartFilePath) {
        List<String> values = CmdExecUtil.runCmd("helm", "show", "values", chartFilePath);
        return String.join("\n", values);
    }

    /**
     * 获取chart包的描述
     *
     * @param chartFilePath chart文件路径
     * @return
     */
    public static Map<String, Object> getInfoMap(String chartFilePath) {
        List<String> results = CmdExecUtil.runCmd("helm", "show", "chart", chartFilePath);
        String infoYaml = String.join("\n", results);
        Yaml yaml = new Yaml();
        return yaml.load(infoYaml);
    }

    /**
     * 获取chart包的描述
     *
     * @param chartFilePath chart文件路径
     * @return
     */
    public static String getDescription(String chartFilePath) {
        Object description = getInfoMap(chartFilePath).get("description");
        return description == null ? null : description.toString();
    }

    /**
     * 获取chart包的模板yaml文件
     *
     * @param chartFilePath chart文件路径
     * @return
     */
    public static Map<String, String> getYamlFileMap(String chartFilePath) {
        List<String> yamlFileList = new ArrayList<>();
        String templatePath = chartFilePath + File.separator + "templates" + File.separator;
        FileUtil.getAllFileName(templatePath, yamlFileList);
        return yamlFileList.stream()
            .filter(item -> StringUtils.endsWith(item, ".yaml") || StringUtils.endsWith(item, ".yml"))
            .collect(Collectors.toMap(item -> StringUtils.substring(item, templatePath.length()), item -> {
                try {
                    return FileUtil.readFile(item);
                } catch (Exception e) {
                    LOGGER.error("读取yaml文件异常：", e);
                    return "";
                }
            }));
    }

    /**
     * 获取chart包的自定义参数文件
     *
     * @param chartFilePath chart文件路径
     * @return
     */
    public static Map<String, String> getParameters(String chartFilePath){
        try {
            Map<String, String> yamlFile = new HashMap<>();
            Map<String, Object> paramFileMap = new HashMap<>();
            String manifestsPath = chartFilePath + File.separator + "manifests" + File.separator;
            List<File> fileList = FileUtil.getFileList(manifestsPath, false);
            fileList.forEach(file -> {
                try {
                    if (!file.getName().endsWith(".yaml")) {
                        return;
                    }
                    if (file.getName().equals("parameters.yaml")) {
                        paramFileMap.put("major", FileUtil.readFile(file.getAbsolutePath()));
                    }
                    String fileStr = FileUtil.readFile(file.getAbsolutePath());
                    JSONObject data = new Yaml().loadAs(fileStr, JSONObject.class);
                    if (data.containsKey("target")) {
                        paramFileMap.put(data.getString("target"), fileStr);
                    }
                } catch (IOException e) {
                    LOGGER.error("读取参数文件{}失败", file.getName());
                }
            });
            yamlFile.put("parameters", MapUtils.hashMapToString(paramFileMap));
            return yamlFile;
        } catch (Exception e){
            LOGGER.error("读取参数文件失败");
            return new HashMap<>();
        }
    }

    /**
     * 获取chart包的自定义参数文件
     *
     * @param chartFilePath chart文件路径
     * @return
     */
    public static Map<String, String> getActiveValues(String chartFilePath){
        try {
            String paramPath = chartFilePath + File.separator + "values-active-active.yaml";
            Map<String, String> yamlFile = new HashMap<>();
            yamlFile.put("values-active-active", FileUtil.readFile(paramPath));
            return yamlFile;
        } catch (Exception e){
            LOGGER.error("读取parameters.yaml文件失败");
            return new HashMap<>();
        }
    }

    /**
     * 获取chart包的operator中的crd文件
     *
     * @param chartFilePath chart文件路径
     * @return
     */
    public static Map<String, String> getCrds(String chartFilePath, String operatorName){
        String path = chartFilePath + File.separator + "charts" + File.separator + operatorName + File.separator;
        if (new File(path + CRD_S).exists()){
            return readFile(path + CRD_S);
        } else if (new File(path + CRD_S_V1).exists()){
            return readFile(path + CRD_S_V1);
        } else if (new File(path + CRD_S_V1BETA1).exists()){
            return readFile(path + CRD_S_V1BETA1);
        }
        return new HashMap<>();
    }

    /**
     * 获取chart包的question.yaml
     *
     * @param chartFilePath chart文件路径
     * @return
     */
    public static String getQuestionYaml(String chartFilePath) throws Exception {
        return FileUtil.readFile(chartFilePath + File.separator + "questions.yaml");
    }

    public static Map<String, String> readFile(String path) {
        List<String> yamlFileList = new ArrayList<>();
        FileUtil.getAllFileName(path, yamlFileList);
        return yamlFileList.stream()
            .filter(item -> StringUtils.endsWith(item, ".yaml") || StringUtils.endsWith(item, ".yml"))
            .collect(Collectors.toMap(item -> StringUtils.substring(item, path.length()), item -> {
                try {
                    return FileUtil.readFile(item);
                } catch (Exception e) {
                    LOGGER.error("读取yaml文件异常：", e);
                    return "";
                }
            }));
    }

}
