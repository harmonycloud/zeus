package com.middleware.zeus.controller.middleware;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.model.middleware.Middleware;
import com.middleware.zeus.annotation.Authority;
import com.middleware.zeus.util.MiddlewareResourceCalculateUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * @author xutianhong
 * @Date 2023/1/13 3:03 下午
 */
@Api(tags = {"服务列表", "服务管理"}, value = "中间件发布资源预览", description = "中间件发布资源预览")
@RestController
@RequestMapping
public class MiddlewarePreInspectionController {

    @ApiOperation(value = "中间件发布资源预览", notes = "中间件发布资源预览")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "middleware", value = "middleware信息", paramType = "query", dataTypeClass = Middleware.class)
    })
    @GetMapping("/middleware/resource/preview")
    @Authority
    public BaseResult<Map<String, Double>> listStatus(@RequestBody Middleware middleware) {
        return BaseResult.ok(MiddlewareResourceCalculateUtil.middlewareResourceCalculate(middleware));
    }

}
