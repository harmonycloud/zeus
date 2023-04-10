package com.middleware.zeus.config;

import java.io.*;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;

import com.middleware.zeus.bean.BeanMiddlewareInfo;
import com.middleware.zeus.dao.BeanMiddlewareInfoMapper;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * @author xutianhong
 * @Date 2021/8/6 4:44 下午
 */
@Slf4j
@Component
public class InitMiddlewareImage {

    @Value("${system.images.path:/usr/local/zeus-pv/images/middleware}")
    private String tempImagePath;

    @Autowired
    private BeanMiddlewareInfoMapper tempMiddlewareInfoMapper;

    private static BeanMiddlewareInfoMapper middlewareInfoMapper;
    private static String imagePath;
    /**
     * 中间件图片名称缓存
     */
    private static Set<String> images = new HashSet<>();

    @PostConstruct
    public void init() throws Exception {
        if (middlewareInfoMapper == null) {
            middlewareInfoMapper = tempMiddlewareInfoMapper;
            imagePath = tempImagePath;
        }
        initImage();
    }

    public void initIfNotExists(String path) {
        try {
            String imageName = path.substring(path.lastIndexOf("/") + 1);
            if (!images.contains(imageName)) {
                initImage();
            }
        } catch (Exception e) {
            log.error("初始化中间件图片失败了", e);
        }
    }

    public void initImage() throws Exception {
        QueryWrapper<BeanMiddlewareInfo> wrapper = new QueryWrapper<>();
        List<BeanMiddlewareInfo> mwInfoList = middlewareInfoMapper.selectList(wrapper);
        for (BeanMiddlewareInfo mwInfo : mwInfoList) {
            if (StringUtils.isEmpty(mwInfo.getImagePath())) {
                continue;
            }
            String filePath = imagePath + File.separator + mwInfo.getImagePath();
            images.add(mwInfo.getImagePath());
            File file = new File(filePath);
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
                    log.info("中间件{} 图片初始化加载成功 path：{}", mwInfo.getChartName() + "-" + mwInfo.getChartVersion(), filePath);
                } catch (Exception e) {
                    log.error("中间件{} 图片初始化加载失败", mwInfo.getChartName() + "-" + mwInfo.getChartVersion(), e);
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
