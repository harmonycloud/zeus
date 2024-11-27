package com.middleware.zeus.common.model;

import com.middleware.zeus.common.model.middleware.MiddlewareResourceInfo;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.List;

/**
 * @author xutianhong
 * @Date 2024/11/26 7:43 PM
 */
@Data
@Accessors(chain = true)
@ApiModel("中间件监控信息查询")
public class MiddlewareResourceQueryDto {

    @ApiModelProperty("目标: cpu/memeory/storage")
    private String target;

    @ApiModelProperty("中间件类型")
    private String type;

    @ApiModelProperty("当前页")
    private Integer current;

    @ApiModelProperty("页大小")
    private Integer size;

    @ApiModelProperty("关键词检索")
    private String keyword;

    @ApiModelProperty("命名空间筛选")
    private List<String> namespaceList;

    @ApiModelProperty("类型筛选")
    private List<String> typeList;

    @ApiModelProperty("排序字段: cpuRequest/cpuUsed/cpuRate/memoryRequest/...")
    private String orderBy;

    @ApiModelProperty("降序/升序: desc/asc")
    private String collation;

    public void sortMiddlewareResourceInfo(List<MiddlewareResourceInfo> mwRsInfoList){
        if (StringUtils.isEmpty(this.orderBy)){
            return;
        }
        switch (this.orderBy){
            case "requestCpu":
                if ("desc".equals(this.collation)) {
                    mwRsInfoList.sort((o1, o2) -> o2.getRequestCpu().compareTo(o1.getRequestCpu()));
                } else {
                    mwRsInfoList.sort(Comparator.comparing(MiddlewareResourceInfo::getRequestCpu));
                }
                break;
            case "per5MinCpu":
                if ("desc".equals(this.collation)) {
                    mwRsInfoList.sort((o1, o2) -> o2.getPer5MinCpu().compareTo(o1.getPer5MinCpu()));
                } else {
                    mwRsInfoList.sort(Comparator.comparing(MiddlewareResourceInfo::getPer5MinCpu));
                }
                break;
            case "cpuRate":
                if ("desc".equals(this.collation)) {
                    mwRsInfoList.sort((o1, o2) -> o2.getCpuRate().compareTo(o1.getCpuRate()));
                } else {
                    mwRsInfoList.sort(Comparator.comparing(MiddlewareResourceInfo::getCpuRate));
                }
                break;
            case "requestMemory":
                if ("desc".equals(this.collation)) {
                    mwRsInfoList.sort((o1, o2) -> o2.getRequestMemory().compareTo(o1.getRequestMemory()));
                } else {
                    mwRsInfoList.sort(Comparator.comparing(MiddlewareResourceInfo::getRequestMemory));
                }
                break;
            case "per5MinMemory":
                if ("desc".equals(this.collation)) {
                    mwRsInfoList.sort((o1, o2) -> o2.getPer5MinMemory().compareTo(o1.getPer5MinMemory()));
                } else {
                    mwRsInfoList.sort(Comparator.comparing(MiddlewareResourceInfo::getPer5MinMemory));
                }
                break;
            case "memoryRate":
                if ("desc".equals(this.collation)) {
                    mwRsInfoList.sort((o1, o2) -> o2.getMemoryRate().compareTo(o1.getMemoryRate()));
                } else {
                    mwRsInfoList.sort(Comparator.comparing(MiddlewareResourceInfo::getMemoryRate));
                }
                break;
            case "requestStorage":
                if ("desc".equals(this.collation)) {
                    mwRsInfoList.sort((o1, o2) -> o2.getRequestStorage().compareTo(o1.getRequestStorage()));
                } else {
                    mwRsInfoList.sort(Comparator.comparing(MiddlewareResourceInfo::getRequestStorage));
                }
                break;
            case "per5MinStorage":
                if ("desc".equals(this.collation)) {
                    mwRsInfoList.sort((o1, o2) -> o2.getPer5MinStorage().compareTo(o1.getPer5MinStorage()));
                } else {
                    mwRsInfoList.sort(Comparator.comparing(MiddlewareResourceInfo::getPer5MinStorage));
                }
                break;
            case "storageRate":
                if ("desc".equals(this.collation)) {
                    mwRsInfoList.sort((o1, o2) -> o2.getStorageRate().compareTo(o1.getStorageRate()));
                } else {
                    mwRsInfoList.sort(Comparator.comparing(MiddlewareResourceInfo::getStorageRate));
                }
                break;
            default:
                break;
        }
    }
}
