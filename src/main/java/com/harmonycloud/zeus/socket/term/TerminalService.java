package com.harmonycloud.zeus.socket.term;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.security.CodeSource;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.LinkedBlockingQueue;

import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.log.LogService;
import com.harmonycloud.zeus.socket.term.helper.ThreadHelper;
import com.harmonycloud.zeus.util.AssertUtil;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmonycloud.caas.common.enums.DictEnum;
import com.harmonycloud.caas.common.model.middleware.LogQueryDto;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterDTO;
import com.pty4j.PtyProcess;
import com.pty4j.WinSize;

@Component
@Scope("prototype")
public class TerminalService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TerminalService.class);

    // 日志最多查询的数量为200
    private static final int DEFAULT_LOG_LINES = 500;

    @Value("${system.upload.path}")
    private String uploadPath;

    @Autowired
    private ClusterService clusterService;
    @Autowired
    private com.harmonycloud.zeus.service.log.TerminalService terminalService;

    private boolean isReady;
    private String[] termCommand;
    private PtyProcess process;
    private Integer columns = 20;
    private Integer rows = 10;
    private BufferedReader inputReader;
    private BufferedReader errorReader;
    private BufferedWriter outputWriter;
    private WebSocketSession webSocketSession;

    private LinkedBlockingQueue<String> commandQueue = new LinkedBlockingQueue<>();

    public void onTerminalInit() {}

    public void onTerminalReady(String container, String pod, String namespace, String clusterId, String scriptType) {

        ThreadHelper.start(() -> {
            isReady = true;
            try {
                initializeProcess(container, pod, namespace, clusterId, scriptType);
            } catch (Exception e) {
                LOGGER.error("服务web控制台初始化失败,namespace:{},pod:{}", namespace, pod, e);
            }
        });

    }

    public void onLogTerminalReady(LogQueryDto logQueryDto) {
        ThreadHelper.start(() -> {
            isReady = true;
            try {
                initializeProcess(logQueryDto);
            } catch (Exception e) {
                LOGGER.error("日志刷新初始化失败,logQueryDto:{}", JSONObject.toJSONString(logQueryDto), e);
            }
        });

    }

    private void initializeProcess(String container, String pod, String namespace, String clusterId, String scriptType)
        throws Exception {
        MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        String userHome = System.getProperty("user.home");

        if (System.getProperties().getProperty("os.name").toUpperCase().indexOf("WINDOWS") > 0) {
            this.termCommand = "cmd.exe".split("\\s+");
        } else {
            String command = "kubectl exec " + pod + " --container=" + container + " -it " + scriptType + " -n "
                + namespace + " --server=" + cluster.getAddress() + " --token=" + cluster.getAccessToken()
                + " --insecure-skip-tls-verify=true";
            LOGGER.info("linux shell command:{}", command);
            this.termCommand = command.split("\\s+");
        }

        Map<String, String> envs = new HashMap<>(System.getenv());
        envs.put("TERM", "xterm");
        if (StringUtils.isBlank(System.getProperty("PTY_LIB_FOLDER"))) {
            System.setProperty("PTY_LIB_FOLDER", uploadPath + "/libpty");
        }
        LOGGER.debug("pty4j lib dir:{}", System.getProperty("PTY_LIB_FOLDER"));
        this.process = PtyProcess.exec(termCommand, envs, userHome);
        process.setWinSize(new WinSize(columns, rows));
        this.inputReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
        this.errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
        this.outputWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), "UTF-8"));

        ThreadHelper.start(() -> {
            printReader(inputReader);
        });

        ThreadHelper.start(() -> {
            printReader(errorReader);
        });

        process.waitFor();

    }

    private void initializeProcess(LogQueryDto logQueryDto) throws Exception {
        AssertUtil.notBlank(logQueryDto.getPod(), DictEnum.POD);
        AssertUtil.notBlank(logQueryDto.getNamespace(), DictEnum.NAMESPACE);
        AssertUtil.notBlank(logQueryDto.getClusterId(), DictEnum.CLUSTER_ID);
        String userHome = System.getProperty("user.home");
        MiddlewareClusterDTO cluster = clusterService.findById(logQueryDto.getClusterId());
        // 标准输出
        String command = "";
        if (LogService.LOG_TYPE_STDOUT.equalsIgnoreCase(logQueryDto.getLogSource())) {
            AssertUtil.notBlank(logQueryDto.getContainer(), DictEnum.CONTAINER);
            command = MessageFormat.format(
                "kubectl logs {0} -c {1} -n {2} --tail={3} -f --server={4} --token={5} --insecure-skip-tls-verify=true",
                logQueryDto.getPod(), logQueryDto.getContainer(), logQueryDto.getNamespace(), DEFAULT_LOG_LINES,
                cluster.getAddress(), cluster.getAccessToken());

        } else if (LogService.LOG_TYPE_LOGFILE.equalsIgnoreCase(logQueryDto.getLogSource())) {
            AssertUtil.notBlank(logQueryDto.getLogDir(), DictEnum.LOG_DIR);
            AssertUtil.notBlank(logQueryDto.getLogFile(), DictEnum.LOG_FILE);
            if (StringUtils.isBlank(logQueryDto.getContainer())) {
                command = MessageFormat.format(
                    "kubectl exec {0} -n {1} --server={2} --token={3} --insecure-skip-tls-verify=true -- tail -f -n {4} {5}/{6}",
                    logQueryDto.getPod(), logQueryDto.getNamespace(), cluster.getAddress(), cluster.getAccessToken(),
                    DEFAULT_LOG_LINES, logQueryDto.getLogDir(), logQueryDto.getLogFile());
            } else {
                command = MessageFormat.format(
                    "kubectl exec {0} -c {1} -n {2} --server={3} --token={4} --insecure-skip-tls-verify=true -- tail -f -n {5} {6}/{7}",
                    logQueryDto.getPod(), logQueryDto.getContainer(), logQueryDto.getNamespace(), cluster.getAddress(),
                    cluster.getAccessToken(), DEFAULT_LOG_LINES, logQueryDto.getLogDir(), logQueryDto.getLogFile());
            }
        }
        LOGGER.info("执行kubectl命令：{}", command);
        this.termCommand = command.split("\\s+");

        Map<String, String> envs = new HashMap<>(System.getenv());
        envs.put("TERM", "xterm");
        if (StringUtils.isBlank(System.getProperty("PTY_LIB_FOLDER"))) {
            System.setProperty("PTY_LIB_FOLDER", uploadPath + "/libpty");
        }
        LOGGER.debug("pty4j lib dir:{}", System.getProperty("PTY_LIB_FOLDER"));
        this.process = PtyProcess.exec(termCommand, envs, userHome);
        process.setWinSize(new WinSize(columns, rows));
        this.inputReader = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8"));
        this.errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), "UTF-8"));
        this.outputWriter = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), "UTF-8"));

        ThreadHelper.start(() -> {
            printReader(inputReader);
        });

        ThreadHelper.start(() -> {
            printReader(errorReader);
        });

        process.waitFor();

    }

    public void print(String text) throws IOException {

        Map<String, String> map = new HashMap<>();
        map.put("type", "TERMINAL_PRINT");
        map.put("text", text);

        String message = new ObjectMapper().writeValueAsString(map);

        webSocketSession.sendMessage(new TextMessage(message));

    }

    private void printReader(BufferedReader bufferedReader) {
        try {
            int nRead;
            char[] data = new char[1 * 1024];
            while ((nRead = bufferedReader.read(data, 0, data.length)) != -1) {
                StringBuilder builder = new StringBuilder(nRead);
                builder.append(data, 0, nRead);
                print(builder.toString());
            }
        } catch (Exception e) {
            LOGGER.warn("printReader失败,destroy process", e);
            process.destroy();
        }
    }

    public void onCommand(String command) throws InterruptedException {

        if (Objects.isNull(command)) {
            return;
        }

        commandQueue.put(command);
        ThreadHelper.start(() -> {
            try {
                outputWriter.write(commandQueue.poll());
                outputWriter.flush();
            } catch (IOException e) {
                LOGGER.warn("onCommand异常", e);
            }
        });

    }

    public void onTerminalResize(String columns, String rows) {
        if (Objects.nonNull(columns) && Objects.nonNull(rows)) {
            this.columns = Integer.valueOf(columns);
            this.rows = Integer.valueOf(rows);

            if (Objects.nonNull(process)) {
                process.setWinSize(new WinSize(this.columns, this.rows));
            }

        }
    }

    public void setWebSocketSession(WebSocketSession webSocketSession) {
        this.webSocketSession = webSocketSession;
    }

    public WebSocketSession getWebSocketSession() {
        return webSocketSession;
    }

    public String getJarContainingFolderPath(Class aclass) throws Exception {
        CodeSource codeSource = aclass.getProtectionDomain().getCodeSource();
        File jarFile;
        if (codeSource.getLocation() != null) {
            jarFile = new File(codeSource.getLocation().toURI());
        } else {
            String path = aclass.getResource(aclass.getSimpleName() + ".class").getPath();
            int startIndex = path.indexOf(":") + 1;
            int endIndex = path.indexOf("!");
            if (startIndex == -1 || endIndex == -1) {
                throw new IllegalStateException(
                    "Class " + aclass.getSimpleName() + " is located not within a jar: " + path);
            }

            String jarFilePath = path.substring(startIndex, endIndex);
            jarFilePath = (new URI(jarFilePath)).getPath();
            jarFile = new File(jarFilePath);
        }

        return jarFile.getParentFile().getAbsolutePath();
    }

    /**
     * 关闭连接时调用 关闭前端页面时需要调用 window.onbeforeunload = function () { ws.close(); }
     */
    public void onTerminalClosed(WebSocketSession session) {
        // Object terminalType = session.getAttributes().get(NameConstant.TERMINAL_TYPE);
        // if (StringUtils.equals((CharSequence)terminalType, NameConstant.TERMINAL_TYPE_CONSOLE)) {
        // String clusterId = (String)session.getAttributes().get(NameConstant.CLUSTER_ID);
        // CurrentUser user = (CurrentUser)session.getAttributes().get(NameConstant.USER);
        // MiddlewareClusterDTO cluster = clusterService.findById(clusterId);
        // try {
        // terminalService.deleteTerminalPods(user, null, cluster);
        // } catch (Exception e) {
        // LOGGER.error("删除console pod失败", e);
        // }
        // }
    }

    /**
     * 删除容器进程
     */
    public void deleteConsoleProcess(WebSocketSession session) {
        terminalService.deleteConsoleProcess(session);
    }
}
