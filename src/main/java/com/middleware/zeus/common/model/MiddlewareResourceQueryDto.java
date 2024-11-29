package com.middleware.zeus.common.model;

import java.util.Comparator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

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
    private Integer current = 1;

    @ApiModelProperty("页大小")
    private Integer size = 5;

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

    public <T extends BaseResourceInfo> void sortMiddlewareResourceInfo(List<T> resourceInfoList) {
        if (StringUtils.isEmpty(orderBy)) {
            return;
        }
        switch (this.orderBy) {
            case "requestCpu":
                if ("desc".equals(this.collation)) {
                    resourceInfoList.sort(Comparator
                        .comparing(BaseResourceInfo::getRequestCpu, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .reversed());
                } else {
                    resourceInfoList.sort(Comparator.comparing(BaseResourceInfo::getRequestCpu,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                }
                break;
            case "per5MinCpu":
                if ("desc".equals(this.collation)) {
                    resourceInfoList.sort(Comparator
                        .comparing(BaseResourceInfo::getPer5MinCpu, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .reversed());
                } else {
                    resourceInfoList.sort(Comparator.comparing(BaseResourceInfo::getPer5MinCpu,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                }
                break;
            case "cpuRate":
                if ("desc".equals(this.collation)) {
                    resourceInfoList.sort(Comparator
                        .comparing(BaseResourceInfo::getCpuRate, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .reversed());
                } else {
                    resourceInfoList.sort(Comparator.comparing(BaseResourceInfo::getCpuRate,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                }
                break;
            case "requestMemory":
                if ("desc".equals(this.collation)) {
                    resourceInfoList.sort(Comparator
                        .comparing(BaseResourceInfo::getRequestMemory, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .reversed());
                } else {
                    resourceInfoList.sort(Comparator.comparing(BaseResourceInfo::getRequestMemory,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                }
                break;
            case "per5MinMemory":
                if ("desc".equals(this.collation)) {
                    resourceInfoList.sort(Comparator
                        .comparing(BaseResourceInfo::getPer5MinMemory, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .reversed());
                } else {
                    resourceInfoList.sort(Comparator.comparing(BaseResourceInfo::getPer5MinMemory,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                }
                break;
            case "memoryRate":
                if ("desc".equals(this.collation)) {
                    resourceInfoList.sort(Comparator
                        .comparing(BaseResourceInfo::getMemoryRate, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .reversed());
                } else {
                    resourceInfoList.sort(Comparator.comparing(BaseResourceInfo::getMemoryRate,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                }
                break;
            case "requestStorage":
                if ("desc".equals(this.collation)) {
                    resourceInfoList.sort(Comparator.comparing(BaseResourceInfo::getRequestStorage,
                        Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
                } else {
                    resourceInfoList.sort(Comparator.comparing(BaseResourceInfo::getRequestStorage,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                }
                break;
            case "per5MinStorage":
                if ("desc".equals(this.collation)) {
                    resourceInfoList.sort(Comparator.comparing(BaseResourceInfo::getPer5MinStorage,
                        Comparator.nullsFirst(Comparator.naturalOrder())).reversed());
                } else {
                    resourceInfoList.sort(Comparator.comparing(BaseResourceInfo::getPer5MinStorage,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                }
                break;
            case "storageRate":
                if ("desc".equals(this.collation)) {
                    resourceInfoList.sort(Comparator
                        .comparing(BaseResourceInfo::getStorageRate, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .reversed());
                } else {
                    resourceInfoList.sort(Comparator.comparing(BaseResourceInfo::getStorageRate,
                        Comparator.nullsLast(Comparator.naturalOrder())));
                }
                break;
            default:
                break;
        }
    }


}
