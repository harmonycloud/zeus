package com.harmonycloud.zeus.controller.k8s;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.Namespace;
import com.harmonycloud.caas.filters.enumm.LanguageEnum;
import com.harmonycloud.zeus.service.k8s.NamespaceService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * @author dengyulong
 * @date 2021/03/25
 */
@Api(tags = {"系统管理","基础资源"}, value = "命名空间", description = "命名空间")
@RestController
@RequestMapping("/clusters/{clusterId}/namespaces")
public class NamespaceController {

    @Autowired
    private NamespaceService namespaceService;
    
    @ApiOperation(value = "查询命名空间列表", notes = "查询命名空间列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "all", value = "是否查询所有命名空间，默认false", paramType = "query", dataTypeClass = Boolean.class),
            @ApiImplicitParam(name = "withQuota", value = "是否返回命名空间配额，默认false", paramType = "query", dataTypeClass = Boolean.class),
            @ApiImplicitParam(name = "withMiddleware", value = "是否返回中间件实例信息，默认false", paramType = "query", dataTypeClass = Boolean.class),
            @ApiImplicitParam(name = "keyword", value = "模糊搜索关键词", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult<List<Namespace>> list(@PathVariable("clusterId") String clusterId,
                                            @RequestParam(value = "all", defaultValue = "false") boolean all,
                                            @RequestParam(value = "withQuota", defaultValue = "false") boolean withQuota,
                                            @RequestParam(value = "withMiddleware", defaultValue = "false") boolean withMiddleware,
                                            @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResult.ok(namespaceService.list(clusterId, all, withQuota, withMiddleware, keyword));
    }

    @ApiOperation(value = "注册命名空间", notes = "注册命名空间")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespaceList", value = "命名空间列表", paramType = "query", dataTypeClass = List.class)
    })
    @PutMapping
    public BaseResult registry(@PathVariable("clusterId") String clusterId,
                               @RequestBody List<String> namespaceList) {
        String msg;
        List<String> failNsList = namespaceService.registry(clusterId, namespaceList);
        if (CollectionUtils.isEmpty(failNsList)) {
            msg = LanguageEnum.isChinese() ? "全部命名空间保存成功" : "These namespaces save successfully";
        } else {
            msg = (LanguageEnum.isChinese() ? "操作失败的命名空间：" : "Here are some namespaces that failed to save: ")
                + failNsList.toString();
        }
        return BaseResult.ok(msg);
    }
    
}
