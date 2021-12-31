package com.harmonycloud.zeus.service.k8s.impl;

import com.harmonycloud.caas.common.enums.ErrorCodeMessage;
import com.harmonycloud.caas.common.exception.CaasRuntimeException;
import com.harmonycloud.zeus.service.k8s.K8sExecService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.text.MessageFormat;

/**
 * @author xutianhong
 * @Date 2021/12/30 2:08 下午
 */
@Slf4j
@Service
public class K8sExecServiceImpl implements K8sExecService {

    @Override
    public void exec(String execCommand){
        boolean error = false;
        Process process = null;
        try {
            log.info("执行kubectl命令：{}", execCommand);
            String[] commands = {"/bin/sh", "-c", execCommand};
            process = Runtime.getRuntime().exec(commands);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(process.getErrorStream()));

            String line;
            while ((line = stdInput.readLine()) != null) {
                log.info("执行指令执行成功:{}", line);
            }

            while ((line = stdError.readLine()) != null) {
                log.error("执行指令错误:{}", line);
                // 过滤一些并非异常的提醒信息
                if (!filter(line)){
                    error = true;
                }
            }
            if (error) {
                throw new Exception();
            }

        } catch (Exception e) {
            log.error("出现异常:", e);
            throw new CaasRuntimeException(String.valueOf(ErrorCodeMessage.RUN_COMMAND_ERROR));
        } finally {
            if (null != process) {
                process.destroy();
            }
        }
    }

    /**
     * 过滤一些并非异常的提醒信息
     **/
    public boolean filter(String line){
        return line.contains("Using a password on the command line interface can be insecure");
    }

}
