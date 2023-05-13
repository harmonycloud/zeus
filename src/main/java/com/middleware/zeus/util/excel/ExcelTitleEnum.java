package com.middleware.zeus.util.excel;


import org.apache.commons.lang3.StringUtils;

/**
 * @author xc
 * @date 2018/8/16 14:52
 */
public enum ExcelTitleEnum {
    // 服务监控导出
    PVC_USAGE("pvcUsage", "存储","Storage"),
    NETWORK_UP("networkUp", "网络(upload)","Network(upload)"),
    NETWORK_DOWN("networkDown", "网络(download)","Network(download)"),
    CPU_USING("cpuUsing", "CPU","CPU"),
    FS_WRITE("fsWrite", "磁盘(write)","Disk(write)"),
    FS_READ("fsRead", "磁盘(read)","Disk(read)"),
    MEMORY_USING("memoryUsing", "内存","Memory"),

    // 主机监控导出
    CLUSTER_CPU_USAGE("clusterCpuUsage", "CPU","CPU"),
    CLUSTER_MEMORY_USAGE("clusterMemoryUsage", "内存","Memory"),
    cluster_Filesystem_Usage("clusterFilesystemUsage", "磁盘","Disk"),

    //导出标题
    HOST_DATA("hostData","主机数据","HostData"),
    TITLE("title","主机数据","Title"),
    DATE("date","主机数据","Date");


    private String prometheusValue;
    private String excelChTitle;
    private String excelEnTitle;

    ExcelTitleEnum(String prometheusValue, String excelChTitle, String excelEnTitle) {
        this.prometheusValue = prometheusValue;
        this.excelChTitle = excelChTitle;
        this.excelEnTitle = excelEnTitle;
    }

    public String getPrometheusValue() {
        return prometheusValue;
    }

    public void setPrometheusValue(String prometheusValue) {
        this.prometheusValue = prometheusValue;
    }

    public String getExcelChTitle() {
        return excelChTitle;
    }

    public void setExcelChTitle(String excelChTitle) {
        this.excelChTitle = excelChTitle;
    }

    public String getExcelEnTitle() {
        return excelEnTitle;
    }

    public void setExcelEnTitle(String excelEnTitle) {
        this.excelEnTitle = excelEnTitle;
    }

    public static String getTitle(String prometheusValue) {
        for (ExcelTitleEnum enumValue : values()) {
            if (StringUtils.equals(enumValue.getPrometheusValue(), prometheusValue)) {
                    return enumValue.getExcelChTitle();
            }
        }
        return null;
    }
}

