package com.harmonycloud.zeus.controller.middleware;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.middleware.ImageRepositoryDTO;
import com.harmonycloud.zeus.service.middleware.ImageRepositoryService;
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
@Api(tags = {"系统管理", "集群管理"}, value = "镜像仓库", description = "镜像仓库")
@RestController
@RequestMapping(value = {"/clusters/{clusterId}/mirror"})
public class ImageRepositoryController {

    @Autowired
    private ImageRepositoryService imageRepositoryService;

    @ApiOperation(value = "查询镜像仓库列表", notes = "查询镜像仓库列表")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "keyword", value = "关键字", paramType = "query", dataTypeClass = String.class)
    })
    @GetMapping
    public BaseResult<List<ImageRepositoryDTO>> list(@PathVariable("clusterId") String clusterId,
                                                     @RequestParam(value = "keyword", required = false) String keyword) {
        return BaseResult.ok(imageRepositoryService.listImageRepository(clusterId, keyword));
    }

    @ApiOperation(value = "新增镜像仓库", notes = "新增镜像仓库")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "mirrorImageDTO", value = "镜像仓库信息", paramType = "query", dataTypeClass = ImageRepositoryDTO.class)
    })
    @PostMapping
    public BaseResult insert(@PathVariable("clusterId") String clusterId,
                             @RequestBody ImageRepositoryDTO imageRepositoryDTO) {
        imageRepositoryService.insert(clusterId, imageRepositoryDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "修改镜像仓库", notes = "修改镜像仓库")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "mirrorImageDTO", value = "镜像仓库信息", paramType = "query", dataTypeClass = ImageRepositoryDTO.class)

    })
    @PutMapping
    public BaseResult update(@PathVariable("clusterId") String clusterId,
                             @RequestBody ImageRepositoryDTO imageRepositoryDTO) {
        imageRepositoryService.update(clusterId, imageRepositoryDTO);
        return BaseResult.ok();
    }

    @ApiOperation(value = "删除镜像仓库", notes = "删除镜像仓库")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "clusterId", value = "集群id", paramType = "path", dataTypeClass = String.class),
            @ApiImplicitParam(name = "id", value = "仓库id", paramType = "query", dataTypeClass = String.class)
    })
    @DeleteMapping
    public BaseResult<List<ImageRepositoryDTO>> delete(@PathVariable("clusterId") String clusterId,
                                                       @RequestParam(value = "id") String id) {
        imageRepositoryService.delete(clusterId, id);
        return BaseResult.ok();
    }
}
