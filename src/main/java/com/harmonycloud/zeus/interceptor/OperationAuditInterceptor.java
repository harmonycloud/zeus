package com.harmonycloud.zeus.interceptor;

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

import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.user.UserDto;
import com.harmonycloud.caas.common.model.user.UserRole;
import com.harmonycloud.caas.filters.user.CurrentUserRepository;
import com.harmonycloud.zeus.bean.user.BeanRoleAuthority;
import com.harmonycloud.zeus.bean.user.BeanUser;
import com.harmonycloud.zeus.service.user.RoleAuthorityService;
import com.harmonycloud.zeus.service.user.UserRoleService;
import com.harmonycloud.zeus.service.user.UserService;
import com.harmonycloud.zeus.util.RequestUtil;
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
import com.harmonycloud.caas.common.base.BaseResult;
import com.harmonycloud.caas.common.constants.CommonConstant;
import com.harmonycloud.caas.filters.token.JwtTokenComponent;
import com.harmonycloud.tool.api.util.HttpMethod;
import com.harmonycloud.zeus.annotation.Authority;
import com.harmonycloud.zeus.bean.BeanOperationAudit;
import com.harmonycloud.zeus.service.system.OperationAuditService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;

import static com.harmonycloud.caas.common.constants.CommonConstant.*;

/**
 * ?????????????????????
 *
 * @author liyinlong
 * @date 2021/7/14 4:19 ??????
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

    @Pointcut("@annotation(io.swagger.annotations.ApiOperation) && (!@annotation(com.harmonycloud.zeus.annotation.ExcludeAuditMethod)) &&(!@annotation(org.springframework.web.bind.annotation.GetMapping))")
    public void pointcut() {

    }

    @Pointcut("@annotation(com.harmonycloud.zeus.annotation.Authority)")
    public void authcut() {

    }

    @Before("authcut() && @annotation(authority)")
    public void before(JoinPoint joinPoint, Authority authority) {
        String projectId = RequestUtil.getProjectId();
        if (StringUtils.isNotEmpty(projectId)) {
            // ??????????????????
            JSONObject userMap = JwtTokenComponent.checkToken(CurrentUserRepository.getUser().getToken()).getValue();
            List<UserRole> userRoleList = userRoleService.get(userMap.getString("username"));
            // ??????????????????????????????
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
     * ??????????????????
     *
     * @param point
     * @return
     * @author liyinlong
     * @date 2021/7/29 3:26 ??????
     */
    @Around("pointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        log.info("??????????????????");
        long beginTimeMillis = System.currentTimeMillis();
        LocalDateTime localDateTime = LocalDateTime.now();
        ZoneId zone = ZoneId.systemDefault();
        Instant instant = localDateTime.atZone(zone).toInstant();
        Date beginTime = Date.from(instant);
        Object result = null;
        try {
            // ????????????
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
                log.error("????????????????????????", e);
            }
        } catch (Throwable e) {
            log.error("??????????????????", e);
            throw e;
        }
        return result;
    }

    /**
     * ??????????????????
     *
     * @param joinPoint   ?????????
     * @param beginTime   ????????????
     * @param actionTime  ????????????
     * @param executeTime ??????(??????)
     * @param result      ????????????
     * @author liyinlong
     * @date 2021/7/21 6:19 ??????
     */
    private void saveOperateLog(ProceedingJoinPoint joinPoint, Date beginTime, Date actionTime, int executeTime, Object result) {
        // ??????HttpServletRequest?????????
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

        // ?????????RequestAttributes?????????HttpServletResponse?????????
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
            // ??????????????????
            operationAudit.setActionChDesc(logAnnotation.value());
        }
        // ??????????????????
        String className = joinPoint.getTarget().getClass().getName();
        String methodName = signature.getName();
        operationAudit.setMethod(className + methodName);

        // ???????????????????????????
        LocalVariableTableParameterNameDiscoverer u = new LocalVariableTableParameterNameDiscoverer();
        String[] paramNames = u.getParameterNames(method);
        // ????????????????????????
        Object[] args = joinPoint.getArgs();

        // ??????????????????
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

        log.info("???????????????{}", operationAudit);
        operationAuditService.insert(operationAudit);
    }

    /**
     * ??????????????????ip
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
     * ??????????????????
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