package com.harmonycloud.zeus.service.log.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.log.TerminalService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.socket.WebSocketSession;

import com.harmonycloud.caas.common.constants.DateStyle;
import com.harmonycloud.caas.common.constants.NameConstant;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.harmonycloud.tool.cmd.CmdExecUtil;
import com.harmonycloud.tool.date.DateUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author dengyulong
 * @date 2021/05/19
 */
@Slf4j
@Service
public class TerminalServiceImpl implements TerminalService {

    @Autowired
    private ClusterService clusterService;

    @Override
    public void deleteConsoleProcess(WebSocketSession session) {

        String terminalType = (String)session.getAttributes().get(NameConstant.TERMINAL_TYPE);
        String clusterId = (String)session.getAttributes().get(NameConstant.CLUSTER_ID);
        String pod = (String)session.getAttributes().get("pod");
        String container = (String)session.getAttributes().get("container");

        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        StringBuilder cmd = new StringBuilder();
        if (StringUtils.equalsIgnoreCase(terminalType, "stdoutlog")) {
            cmd.append("ps -ef|grep 'kubectl logs ");
            cmd.append(pod).append(" -c ").append(container);
            cmd.append("'|grep '").append(cluster.getHost());
            cmd.append("'|grep  -v 'grep'|awk '{print $2,$5,$8}'");
            deleteConsoleProcesses(cmd.toString(), 0);
        } else if (StringUtils.equalsIgnoreCase(terminalType, "filelog")) {
            cmd.append("ps -ef|grep 'kubectl exec ");
            cmd.append(pod);
            if (StringUtils.isNotBlank(container)) {
                cmd.append(" -c ").append(container);
            }
            cmd.append("'|grep '").append(cluster.getHost());
            cmd.append("'|grep  -v 'grep'|awk '{print $2,$5,$8}'");
            deleteConsoleProcesses(cmd.toString(), 0);
        } else if (StringUtils.equalsIgnoreCase(terminalType, "console")) {
            cmd.append("ps -ef|grep 'kubectl exec ");
            cmd.append(pod).append(" --container=").append(container);
            cmd.append("'|grep '").append(cluster.getHost());
            cmd.append("'|grep  -v 'grep'|awk '{print $2,$5,$8}'");
            deleteConsoleProcesses(cmd.toString(), 0);
        } else if (terminalType == null) {
            cmd.append("ps -ef|grep 'kubectl exec ");
            cmd.append(pod).append(" --container=").append(container);
            cmd.append("'|grep '").append(cluster.getHost());
            cmd.append("'|grep  -v 'grep'|awk '{print $2,$5,$8}'");
            deleteConsoleProcesses(cmd.toString(), 0);
        }
    }

    /**
     * 删除控制台线程
     *
     * @Author liusenze
     * @Date 2020/6/19 11:22 上午
     * @param cmd
     * @param limitHour
     * @return void
     */
    private void deleteConsoleProcesses(String cmd, Integer limitHour) {
        List<String> processList = null;
        try {
            processList = CmdExecUtil.runCmd("/bin/sh", "-c", cmd);
        } catch (Exception e) {
            e.printStackTrace();
        }
        List<String> pidList = filter(processList, limitHour);

        log.info("删除控制台进程 {}", String.join(",", pidList));
        pidList.forEach(item -> {
            try {
                CmdExecUtil.runCmd("/bin/sh", "-c", "kill -9 " + item);
            } catch (Exception e) {
                log.error("删除控制台进程失败 {}", item, e);
            }
        });
    }

    /**
     * 过滤创建超过限制时间的进程pid
     */
    private List<String> filter(List<String> processList, Integer limitHour) {
        if (CollectionUtils.isEmpty(processList)) {
            return new ArrayList<>();
        }

        Date now = new Date();
        List<String[]> list = processList.stream().map(item -> item.split(" ")).collect(Collectors.toList());

        return list.stream().filter(item -> createTimeBeforeOneHour(item[1], now, limitHour)).map(item -> item[0])
            .collect(Collectors.toList());
    }

    /**
     * 校验线程时间是否超出限制时间
     */
    private boolean createTimeBeforeOneHour(String time, Date now, Integer limitHour) {
        if (!StringUtils.contains(time, ":")) {
            return true;
        }
        String prefix = DateUtils.dateToString(now, DateStyle.YYYY_MM_DD);
        Date newTime = DateUtils.parseDate(prefix + " " + time, DateStyle.YYYY_MM_DD_HH_MM);
        // 兼容mac系统
        if (StringUtils.endsWith(time, "PM")) {
            newTime = DateUtils.addInteger(newTime, Calendar.HOUR_OF_DAY, 12);
        }
        return DateUtils.addInteger(newTime, Calendar.HOUR_OF_DAY, limitHour).before(now);
    }

}
