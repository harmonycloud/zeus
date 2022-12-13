package com.middleware.zeus.interceptor;

import java.lang.reflect.Method;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.model.user.UserDto;
import com.middleware.caas.common.model.user.UserRole;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.middleware.zeus.bean.user.BeanRoleAuthority;
import com.middleware.zeus.service.user.RoleAuthorityService;
import com.middleware.zeus.service.user.UserRoleService;
import com.middleware.zeus.service.user.UserService;
import com.middleware.zeus.util.RequestUtil;
import com.middleware.zeus.annotation.Authority;
import com.middleware.zeus.bean.BeanOperationAudit;
import com.middleware.zeus.service.system.OperationAuditService;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.LocalVariableTableParameterNameDiscoverer;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.middleware.caas.common.base.BaseResult;
import com.middleware.caas.common.constants.CommonConstant;
import com.harmonycloud.caas.filters.token.JwtTokenComponent;
import com.middleware.tool.api.util.HttpMethod;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import static com.middleware.caas.common.constants.CommonConstant.*;

/**
 * 操作审计拦截器
 *
 * @author liyinlong
 * @date 2021/7/14 4:19 下午
 */
@Aspect
@Component
@Slf4j
public class OperationAuditInterceptor {

    @Autowired
    private OperationAuditService operationAuditService;
    @Autowired
    private UserRoleService userRoleService;
    @Autowired
    private RoleAuthorityService roleAuthorityService;
    @Autowired
    private UserService userService;

    @Pointcut("@annotation(io.swagger.annotations.ApiOperation) && (!@annotation(com.middleware.zeus.annotation.ExcludeAuditMethod)) &&(!@annotation(org.springframework.web.bind.annotation.GetMapping))")
    public void pointcut() {

    }

    @Pointcut("@annotation(com.middleware.zeus.annotation.Authority)")
    public void authcut() {

    }

    @Before("authcut() && @annotation(authority)")
    public void before(JoinPoint joinPoint, Authority authority) {
        String projectId = RequestUtil.getProjectId();
        if (StringUtils.isNotEmpty(projectId)) {
            // 校验角色权限
            JSONObject userMap = JwtTokenComponent.checkToken(CurrentUserRepository.getUser().getToken()).getValue();
            List<UserRole> userRoleList = userRoleService.get(userMap.getString("username"));
            // 判断是否为超级管理员
            boolean notAdmin = CollectionUtils.isEmpty(
                    userRoleList.stream().filter(userRole -> userRole.getRoleId() == 1).collect(Collectors.toList()));
            if (notAdmin) {
                userRoleList = userRoleList.stream().filter(userRole -> StringUtils.isNotEmpty(userRole.getProjectId())
                        && userRole.getProjectId().equals(projectId)).collect(Collectors.toList());
                if (CollectionUtils.isEmpty(userRoleList)) {
                    throw new BusinessException(ErrorMessage.NO_AUTHORITY);
                }
                UserRole userRole = userRoleList.get(0);
                if (userRole.getRoleId() != 1) {
                    JSONObject params =
                            getParams(((MethodSignature)joinPoint.getSignature()).getParameterNames(), joinPoint.getArgs());
                    String type = tryGetType(params);
                    if (StringUtils.isNotEmpty(type)) {
                        List<BeanRoleAuthority> beanRoleAuthorityList = roleAuthorityService.list(userRole.getRoleId())
                                .stream().filter(beanRoleAuthority -> beanRoleAuthority.getType().equals(type))
                                .collect(Collectors.toList());
                        if (!CollectionUtils.isEmpty(beanRoleAuthorityList)) {
                            String[] power = beanRoleAuthorityList.get(0).getPower().split("");
                            if (Integer.parseInt(power[authority.power()]) == 0) {
                                throw new BusinessException(ErrorMessage.NO_AUTHORITY);
                            }
                        }
                    }
                }
            }
        }
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
        LocalDateTime localDateTime = LocalDateTime.now();
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = localDateTime.atZone(zone).toInstant();
        Date beginTime = Date.from(instant);
        Object result = null;
        try {
            // 执行方法
            result = point.proceed();

            Long actionTimeMillis = System.currentTimeMillis();
            Long executeTime = (actionTimeMillis - beginTimeMillis);
            LocalDateTime time = LocalDateTime.now();
            Instant ins = time.atZone(zone).toInstant();
            Date actionTime = Date.from(ins);

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
    private void saveOperateLog(ProceedingJoinPoint joinPoint, Date beginTime, Date actionTime, int executeTime, Object result) {
        // 获取HttpServletRequest的信息
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        if (HttpMethod.GET.equalsIgnoreCase(request.getMethod())) {
            return;
        }

        BeanOperationAudit operationAudit = new BeanOperationAudit();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Class<?> aClass = joinPoint.getTarget().getClass();

        operationAudit.setUrl(request.getRequestURI());
        setRealIp(request, operationAudit);
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
        }

        // 从获取RequestAttributes中获取HttpServletResponse的信息
        HttpServletResponse response = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
        operationAudit.setStatus(String.valueOf(response.getStatus()));
        JwtTokenComponent.JWTResultEnum resultEnum = JwtTokenComponent.checkToken(request.getHeader("userToken"));
        JSONObject userJson = resultEnum.getValue();

        if (userJson != null) {
            setUserRole(userJson.getString("username"), operationAudit);
            operationAudit.setAccount(userJson.getString("username"));
            operationAudit.setUserName(userJson.getString("aliasName"));
            operationAudit.setPhone(userJson.getString("phone"));
        } else {
            if (result != null && result instanceof BaseResult) {
                BaseResult<JSONObject> baseResult = (BaseResult<JSONObject>) result;
                JSONObject resultData = baseResult.getData();
                if (resultData != null) {
                    String token = resultData.getString("token");
                    resultEnum = JwtTokenComponent.checkToken(token);
                    userJson = resultEnum.getValue();
                    setUserRole(userJson.getString("username"), operationAudit);
                    operationAudit.setAccount(userJson.getString("username"));
                    operationAudit.setUserName(userJson.getString("aliasName"));
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

        log.info("操作日志：{}", operationAudit);
        operationAuditService.insert(operationAudit);
    }

    /**
     * 设置真实请求ip
     * @param request
     * @param operationAudit
     */
    public void setRealIp(HttpServletRequest request, BeanOperationAudit operationAudit) {
        String remoteRealIp = request.getHeader("x-forwarded-for");
        log.info("x-forwarded-for:{}", remoteRealIp);
        log.info("remoteHost:{}", request.getRemoteHost());
        if (remoteRealIp != null && !StringUtils.isBlank(remoteRealIp)) {
            operationAudit.setRemoteIp(remoteRealIp);
        } else {
            operationAudit.setRemoteIp(request.getRemoteHost());
        }
    }

    public JSONObject getParams(String[] paramNames, Object[] args) {
        JSONObject params = new JSONObject();
        for (int i = 0; i < paramNames.length; ++i) {
            if (args[i] instanceof String) {
                params.put(paramNames[i], args[i]);
            } else {
                params.put(paramNames[i], JSONObject.parseObject(JSONObject.toJSONString(args[i])));
            }

        }
        return params;
    }

    public String tryGetType(JSONObject params) {
        String type = null;
        if (params.containsKey(TYPE)) {
            type = params.getString(TYPE);
        } else {
            for (String key : params.keySet()) {
                if (params.get(key) instanceof JSONObject) {
                    JSONObject temp = params.getJSONObject(key);
                    if (temp.containsKey(TYPE)) {
                        type = temp.getString(TYPE);
                    } else if (temp.containsKey(MIDDLEWARE_TYPE)) {
                        type = temp.getString(MIDDLEWARE_TYPE);
                    }
                }
            }
        }
        return type;
    }

    /**
     * 设置用户角色
     */
    private void setUserRole(String username, BeanOperationAudit operationAudit) {
        UserDto userDto = userService.getUserDto(username);
        if (userDto == null) {
            return;
        }
        if (!CollectionUtils.isEmpty(userDto.getUserRoleList())) {
            operationAudit.setRoleName(userDto.getUserRoleList().get(0).getRoleName());
        }
    }

}