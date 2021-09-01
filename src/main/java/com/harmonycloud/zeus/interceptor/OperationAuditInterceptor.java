package com.harmonycloud.zeus.interceptor;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.constants.CommonConstant;
import com.harmonycloud.caas.filters.token.JwtTokenComponent;
import com.harmonycloud.zeus.bean.BeanOperationAudit;
import com.harmonycloud.zeus.service.system.OperationAuditService;
import com.harmonycloud.tool.api.util.HttpMethod;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 操作审计拦截器
 *
 * @author liyinlong
 * @date 2021/7/14 4:19 下午
 */
@Aspect
@Component
public class OperationAuditInterceptor {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private OperationAuditService operationAuditService;

    @Pointcut("@annotation(io.swagger.annotations.ApiOperation) && (!@annotation(com.harmonycloud.zeus.annotation.ExcludeAuditMethod)) &&(!@annotation(org.springframework.web.bind.annotation.GetMapping))")
    public void pointcut() {

    }

    /**
     * 执行环绕通知
     *
     * @param point
     * @return
     * @author liyinlong
     * @date 2021/7/29 3:26 下午
     */
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        log.info("记录操作日志");
        long beginTimeMillis = System.currentTimeMillis();
        LocalDateTime beginTime = LocalDateTime.now();
        Object result = null;
        try {
            // 执行方法
            result = point.proceed();

            Long actionTimeMillis = System.currentTimeMillis();
            Long executeTime = (actionTimeMillis - beginTimeMillis);
            LocalDateTime actionTime = LocalDateTime.now();

            try {
                saveOperateLog(point, beginTime, actionTime, executeTime.intValue(), result);
            } catch (Exception e) {
                e.printStackTrace();
                log.error("操作审计保存失败", e);
            }
        } catch (Throwable e) {
            log.error("请求处理失败", e);
            throw e;
        }
        return result;
    }

    /**
     * 保存操作日志
     *
     * @param joinPoint   切入点
     * @param beginTime   请求时间
     * @param actionTime  响应时间
     * @param executeTime 耗时(毫秒)
     * @param result      请求结果
     * @author liyinlong
     * @date 2021/7/21 6:19 下午
     */
    private void saveOperateLog(ProceedingJoinPoint joinPoint, LocalDateTime beginTime, LocalDateTime actionTime, int executeTime, Object result) {
        // 获取HttpServletRequest的信息
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        if (HttpMethod.GET.equalsIgnoreCase(request.getMethod())) {
            return;
        }

        BeanOperationAudit operationAudit = new BeanOperationAudit();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> aClass = joinPoint.getTarget().getClass();

        operationAudit.setUrl(request.getRequestURI());
        operationAudit.setRemoteIp(getIPAddress(request));
        operationAudit.setRequestMethod(request.getMethod());

        if (result != null) {
            BaseResult baseResult = (BaseResult) result;
            if (baseResult.getData() != null) {
                if (baseResult.getData().toString().length() > CommonConstant.CHAR_2KB) {
                    operationAudit.setResponse(baseResult.getData().toString().substring(0, CommonConstant.CHAR_2KB));
                } else {
                    operationAudit.setResponse(JSON.toJSONString(baseResult.getData()));
                }
            }
            operationAudit.setStatus(baseResult.getSuccess().toString());
        }

        JwtTokenComponent.JWTResultEnum resultEnum = JwtTokenComponent.checkToken(request.getHeader("userToken"));
        JSONObject userJson = resultEnum.getValue();

        if (userJson != null) {
            operationAudit.setAccount(userJson.getString("username"));
            operationAudit.setUserName(userJson.getString("aliasName"));
            operationAudit.setRoleName(userJson.getString("roleName"));
            operationAudit.setPhone(userJson.getString("phone"));
        } else {
            if (result != null && result instanceof BaseResult) {
                BaseResult<JSONObject> baseResult = (BaseResult<JSONObject>) result;
                JSONObject resultData = baseResult.getData();
                if (resultData != null) {
                    String token = resultData.getString("token");
                    resultEnum = JwtTokenComponent.checkToken(token);
                    userJson = resultEnum.getValue();
                    operationAudit.setAccount(userJson.getString("username"));
                    operationAudit.setUserName(userJson.getString("aliasName"));
                    operationAudit.setRoleName(userJson.getString("roleName"));
                    operationAudit.setPhone(userJson.getString("phone"));
                }
            }
        }

        operationAudit.setBeginTime(beginTime);
        operationAudit.setActionTime(actionTime);
        operationAudit.setExecuteTime(executeTime);

        Api annotation = aClass.getAnnotation(Api.class);
        String[] tags = annotation.tags();
        if (tags != null && tags.length > 0) {
            operationAudit.setModuleChDesc(tags[0]);
            if (tags.length > 1) {
                operationAudit.setChildModuleChDesc(tags[1]);
            }
        }

        Method method = signature.getMethod();
        ApiOperation logAnnotation = method.getAnnotation(ApiOperation.class);
        if (logAnnotation != null) {
            // 注解上的描述
            operationAudit.setActionChDesc(logAnnotation.value());
        }
        // 请求的方法名
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();
        operationAudit.setMethod(className + methodName);

        // 请求的方法参数名称
        LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
        String[] paramNames = u.getParameterNames(method);
        // 请求的方法参数值
        Object[] args = joinPoint.getArgs();

        // 拼接请求参数
        if (args != null && paramNames != null) {
            Map<String, Object> paramJson = new HashMap<>(8);
            for (int i = 0; i < args.length; i++) {
                paramJson.put(paramNames[i], args[i]);
                if ("clusterId".equals(paramNames[i])) {
                    operationAudit.setClusterId(args[i].toString());
                }
            }
            operationAudit.setRequestParams(paramJson.toString());
        }

        log.info("操作日志：" + operationAudit);
        operationAuditService.insert(operationAudit);
    }

    /**
     * @description 获取真实ip
     * @author  liyinlong
     * @since 2021/9/1 11:46 上午
     * @param request
     * @return
     */
    public String getIPAddress(HttpServletRequest request) {
        String ipAddresses = request.getHeader("X-Forwarded-For");
        log.info("X-Forwarded-For:{}", ipAddresses);
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String element = headerNames.nextElement();
            String header = request.getHeader(element);
            log.info("headerName:{}--->headerValue={}", element, header);
        }

        if (ipAddresses != null) {
            return ipAddresses;
        }
        return request.getRemoteAddr();
    }
}