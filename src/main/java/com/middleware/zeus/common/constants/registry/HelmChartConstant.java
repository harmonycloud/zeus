package com.middleware.zeus.common.constants.registry;

/**
 * @author dengyulong
 * @date 2020/12/22
 * helm chart 相关常量
 */
public class HelmChartConstant {

    public static final String TGZ = "tgz";

    public static final String CHART_YAML_NAME = "Chart.yaml";
    public static final String VALUES_YAML_NAME = "values.yaml";
    public static final String CHART_REPO_DIR_NAME = "chartrepo";
    public static final String TEMPLATES = "templates";

    public static final String HELM_RELEASE_ANNOTATION_KEY = "meta.helm.sh/release-name";
    public static final String HELM_RELEASE_LABEL_KEY = "app.kubernetes.io/managed-by";
    public static final String HELM_RELEASE_LABEL_VALUE = "Helm";

    public static final String MANIFESTS = "manifests";
    public static final String ICON_SVG = "icon.svg";
    public static final String ICON = "icon";
    public static final String SVG = "svg";
}
