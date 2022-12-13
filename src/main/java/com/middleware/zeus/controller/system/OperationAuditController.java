package com.middleware.zeus.controller.system;

import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.zeus.annotation.ExcludeAuditMethod;
import com.middleware.zeus.bean.OperationAuditQueryDto;
import com.middleware.zeus.service.system.OperationAuditService;
import io.swagger.annotations.Api;
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
@Api(tags = {"系统管理", "操作审计"}, value = "操作审计")
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
    public BaseResult listOperationAudit(@RequestBody OperationAuditQueryDto operationAuditQueryDto) {
        try {
            log.info("查询操作审计列表:{}", operationAuditQueryDto);
            return operationAuditService.list(operationAuditQueryDto);
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
    public BaseResult listAllCondition() {
        try {
            log.info("查询操作审计菜单:{}");
            return operationAuditService.listAllCondition();
        } catch (Exception e) {
            e.printStackTrace();
            log.error("查询操作审计菜单失败：", e);
            return BaseResult.error(ErrorMessage.UNKNOWN);
        }
    }



}
