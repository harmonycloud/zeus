package com.middleware.zeus.util.cmd;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import com.middleware.zeus.common.enums.ErrorMessage;
import com.middleware.zeus.common.exception.BusinessException;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.middleware.zeus.common.constants.CommonConstant.RESOURCE_ALREADY_EXISTED;

/**
 * @author dengyulong
 * @date 2020/2/12
 */
public class CmdExecUtil {

    private static final Logger logger = LoggerFactory.getLogger(CmdExecUtil.class);

    /**
     * 运行shell命令
     * 
     * @param commandArray 命令
     * @return
     */
    public static List<String> runCmd(String... commandArray) {
        return runCmd(true, commandArray);
    }
    /**
     * 运行shell命令
     *
     * @param isErrorThrow 是否错误时抛出异常
     * @param commandArray 命令
     * @return
     */
    public static List<String> runCmd(boolean isErrorThrow, String... commandArray) {
        List<String> resList = new ArrayList<>();
        execCmd(buildCmdStr(commandArray), inputMsg -> {
            resList.add(inputMsg);
            return inputMsg;
        }, errorMsg -> {
            // 之后会被catch到
            if (!isErrorThrow) {
                return errorMsg;
            }
            throw new RuntimeException(errorMsg);
        });
        return resList;
    }

    public static List<String> execCmd(String cmd, Function<String, String> dealWithErrMsg) {
        List<String> res = new ArrayList<>();
        try {
            CmdExecUtil.execCmd(cmd, inputMsg -> {
                res.add(inputMsg);
                return inputMsg;
            }, dealWithErrMsg == null ? warningMsg() : dealWithErrMsg);
        } catch (Exception e) {
            if (StringUtils.isNotEmpty(e.getMessage()) && e.getMessage().contains(RESOURCE_ALREADY_EXISTED)) {
                logger.error(e.getMessage());
                throw new BusinessException(ErrorMessage.RESOURCE_ALREADY_EXISTED);
            } else {
                throw e;
            }
        }
        return res;
    }

    public static String buildCmdStr(String[] command) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String s : command) {
            stringBuilder.append(s).append(" ");
        }
        return stringBuilder.toString();
    }
    
    /**
     * 执行命令
     *
     * @param cmd 执行命令
     * @param dealInput 成功处理
     * @param dealErr 失败处理
     */
    public static void execCmd(String cmd, Function<String, String> dealInput, Function<String, String> dealErr) {
        Process p = null;
        try {
            String[] cmdArr = new String[] {"/bin/sh", "-c", cmd};
            logger.debug("执行命令 : {}", Arrays.toString(cmdArr));
            p = Runtime.getRuntime().exec(cmdArr);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String res;
            while ((res = stdInput.readLine()) != null) {
                dealInput.apply(res);
            }
            while ((res = stdError.readLine()) != null) {
                dealErr.apply(res);
            }
            int runningStatus = p.waitFor();
            logger.debug("执行状态 : {}", runningStatus);
        } catch (Exception e) {
            logger.error("执行语句{} 出现异常", cmd);
            logger.debug("执行语句{} 出现异常", cmd, e);
            throw new RuntimeException("Run command error : " + e.getMessage());
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }

    private static Function<String, String> warningMsg() {
        return errorMsg -> {
            if (errorMsg.startsWith("WARNING: ") || errorMsg.contains("warning: ")) {
                return errorMsg;
            }
            if (errorMsg.contains("OperatorConfiguration") || errorMsg.contains("operatorconfigurations")){
                return errorMsg;
            }
            if (errorMsg.contains("CustomResourceDefinition is deprecated") || errorMsg.contains("apiextensions.k8s.io/v1beta1")){
                return errorMsg;
            }
            if (errorMsg.contains("PodSecurityPolicy is deprecated")){
                return errorMsg;
            }
            if (errorMsg.contains("CSIDriver is deprecated")){
                return errorMsg;
            }
            if (errorMsg.contains("Warning: Use tokens from the TokenRequest API or manually created secret-based tokens instead of auto-generated secret-based tokens.")){
                return errorMsg;
            }
            throw new RuntimeException(errorMsg);
        };
    }

}
