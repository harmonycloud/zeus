package com.middleware.zeus.controller.system;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.middleware.zeus.common.base.BaseResult;
import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.annotation.ExcludeAuditMethod;
import com.middleware.zeus.bean.BeanOperationAudit;
import com.middleware.zeus.bean.OperationAuditQueryDto;
import com.middleware.zeus.common.model.OperationAuditConditionDto;
import com.middleware.zeus.service.system.OperationAuditService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 操作审计
 *
 * @author liyinlong
 * @date 2021/7/27 2:42 下午
 */
@Api(tags = {"平台管理", "操作审计"}, value = "操作审计")
@RestController
@RequestMapping("/operationAudit")
public class OperationAuditController {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private OperationAuditService operationAuditService;

    /**
     * 查询操作审计列表
     * @author liyinlong
     * @date 2021/7/27 4:19 下午
     * @param operationAuditQueryDto 查询条件
     * @return
     */
    @ExcludeAuditMethod
    @ApiOperation(value = "查询操作审计列表", notes = "查询操作审计")
    @ResponseBody
    @RequestMapping(method = RequestMethod.POST)
    public BaseResult<Page<BeanOperationAudit>> listOperationAudit(@RequestBody OperationAuditQueryDto operationAuditQueryDto) {
        try {
            log.info("查询操作审计列表:{}", operationAuditQueryDto);
            return BaseResult.ok(operationAuditService.list(operationAuditQueryDto));
        } catch (Exception e) {
            e.printStackTrace();
            log.error("查询操作审计列表失败：", e);
            return BaseResult.error(ErrorMessage.UNKNOWN);
        }
    }

    /**
     * 查询操作审计菜单
     * @author liyinlong
     * @date 2021/7/30 9:38 上午
     * @return
     */
    @ApiOperation(value = "查询操作审计菜单", notes = "查询操作审计菜单")
    @ResponseBody
    @RequestMapping(value = "listAllCondition",method = RequestMethod.GET)
    public BaseResult<OperationAuditConditionDto> listAllCondition() {
        try {
            log.info("查询操作审计菜单:{}");
            return BaseResult.ok(operationAuditService.listAllCondition());
        } catch (Exception e) {
            e.printStackTrace();
            log.error("查询操作审计菜单失败：", e);
            return BaseResult.error(ErrorMessage.UNKNOWN);
        }
    }

    @ApiOperation(value = "查询操作审计详情", notes = "查询操作审计详情")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "auditId", value = "审计id", paramType = "path", dataTypeClass = String.class),
    })
    @ResponseBody
    @RequestMapping(value = "/{auditId}",method = RequestMethod.GET)
    public BaseResult<BeanOperationAudit> get(@PathVariable("auditId") Integer auditId) {
        return BaseResult.ok(operationAuditService.get(auditId));
    }

}
