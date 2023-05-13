package com.middleware.zeus.util.cmd;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
                resList.add(errorMsg);
                return errorMsg;
            }
            throw new RuntimeException(errorMsg);
        });
        return resList;
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
            logger.info("执行命令 : {}", Arrays.toString(cmdArr));
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
            logger.info("执行状态 : {}", runningStatus);
        } catch (Exception e) {
            logger.error("执行异常", e);
            throw new RuntimeException("Run command error : " + e.getMessage());
        } finally {
            if (p != null) {
                p.destroy();
            }
        }
    }

}
