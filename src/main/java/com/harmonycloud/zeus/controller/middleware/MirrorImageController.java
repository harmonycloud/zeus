package com.harmonycloud.zeus.controller.middleware;

import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.model.middleware.MirrorImageDTO;
import com.harmonycloud.zeus.service.middleware.MirrorImageService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @author yushuaikang
 * @date 2022/3/11 上午10:44
 */
@Api(tags = {"资源池"}, value = "镜像仓库", description = "镜像仓库")
@RestController
@RequestMapping(value = {"/clusters/{clusterId}/namespaces/{namespace}/mirror"})
public class MirrorImageController {

    @Autowired
    private MirrorImageService mirrorImageService;

    @ApiOperation(value = "查询镜像仓库列表", notes = "查询镜像仓库列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyword", value = "关键字", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult<List<MirrorImageDTO>> list(@PathVariable("clusterId") String clusterId,
                                                          @PathVariable(value = "namespace") String namespace,
                                                          @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResult.ok(mirrorImageService.listMirrorImages(clusterId, namespace, keyword));
    }

    @ApiOperation(value = "新增镜像仓库", notes = "新增镜像仓库")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "mirrorImageDTO", value = "镜像仓库信息", paramType = "query", dataTypeClass = MirrorImageDTO.class)
    })
    @PostMapping
    public BaseResult insert(@PathVariable("clusterId") String clusterId,
                                                          @PathVariable(value = "namespace") String namespace,
                                                          @RequestBody MirrorImageDTO mirrorImageDTO) {
        mirrorImageService.insert(clusterId, namespace, mirrorImageDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "修改镜像仓库", notes = "修改镜像仓库")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "mirrorImageDTO", value = "镜像仓库信息", paramType = "query", dataTypeClass = MirrorImageDTO.class)

    })
    @PutMapping
    public BaseResult update(@PathVariable("clusterId") String clusterId,
                             @PathVariable(value = "namespace") String namespace,
                             @RequestBody MirrorImageDTO mirrorImageDTO) {
        mirrorImageService.update(clusterId, namespace, mirrorImageDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除镜像仓库", notes = "删除镜像仓库")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "namespace", value = "命名空间", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "id", value = "仓库id", paramType = "query", dataTypeClass = String.class)
    })
    @DeleteMapping
    public BaseResult<List<MirrorImageDTO>> delete(@PathVariable("clusterId") String clusterId,
                                                 @PathVariable(value = "namespace") String namespace,
                                                 @RequestParam(value = "id") String id) {
        mirrorImageService.delete(clusterId, namespace, id);
        return BaseResult.ok();
    }
}
