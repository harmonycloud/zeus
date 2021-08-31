package com.harmonycloud.zeus.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import javax.annotation.PostConstruct;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2021/8/6 4:44 下午
 */
@Slf4j
@Component
public class InitMiddlewareImage {

    @Value("${system.images.path:/usr/local/zeus-pv/images/middleware}")
    private String imagePath;

    @Autowired
    private MiddlewareInfoService middlewareInfoService;

    @PostConstruct
    public void init() throws Exception {
        List<BeanMiddlewareInfo> mwInfoList = middlewareInfoService.list(null);
        for (BeanMiddlewareInfo mwInfo : mwInfoList) {
            if (StringUtils.isEmpty(mwInfo.getImagePath())){
                return;
            }
            File file = new File(imagePath + File.separator + mwInfo.getImagePath());
            if (!file.exists()) {
                InputStream in = new ByteArrayInputStream(mwInfo.getImage());
                FileOutputStream fileOutputStream = null;
                try {
                    fileOutputStream = new FileOutputStream(file);
                    byte[] buf = new byte[mwInfo.getImage().length];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        fileOutputStream.write(buf, 0, len);
                    }
                    fileOutputStream.flush();
                } catch (Exception e) {
                    log.error("中间件{} 图片初始化加载失败", mwInfo.getChartName() + "-" + mwInfo.getChartVersion());
                } finally {
                    in.close();
                    if (fileOutputStream != null) {
                        fileOutputStream.close();
                    }
                }
            }
        }
    }

}
