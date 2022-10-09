package com.harmonycloud.zeus.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.zeus.bean.PersonalizedConfiguration;
import com.harmonycloud.zeus.dao.user.PersonalMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * @author yushuaikang
 * @date 2022/1/8 上午11:29
 */
@Slf4j
@Component
public class InitPersonalImage {

    @Value("${system.images.path:/usr/local/zeus-pv/}")
    private String tempImagePath;

    @Autowired
    private PersonalMapper temPersonalMapper;

    private static PersonalMapper personalMapper;
    private static String imagePath;
    /**
     * 中间件图片名称缓存
     */
    private static Set<String> images = new HashSet<>();

    @PostConstruct
    public void init() throws Exception {
        if (personalMapper == null) {
            personalMapper = temPersonalMapper;
            imagePath = tempImagePath;
        }
        initPersonal();
    }

    public void initPersonal() throws Exception {
        //判断是否存在初始化数据
        QueryWrapper<PersonalizedConfiguration> wrapper = new QueryWrapper<>();
        List<PersonalizedConfiguration> personals = personalMapper.selectList(wrapper);
        if (personals.stream().anyMatch(per -> "0".equals(per.getStatus()))) {
            return;
        }
        //
        InputStream backIs = InitPersonalImage.class.getClassLoader().getResourceAsStream("images/background.svg");
        InputStream homeIs = InitPersonalImage.class.getClassLoader().getResourceAsStream("images/homelogo.svg");
        InputStream loginIs = InitPersonalImage.class.getClassLoader().getResourceAsStream("images/loginlogo.svg");
        InputStream tabIs = InitPersonalImage.class.getClassLoader().getResourceAsStream("images/tablogo.svg");
        PersonalizedConfiguration personal = new PersonalizedConfiguration();
        personal.setBackgroundPath("background.svg");
        personal.setBackgroundImage(loadFile(backIs,"background.svg"));
        personal.setHomeLogoPath("homelogo.svg");
        personal.setHomeLogo(loadFile(homeIs,"homelogo.svg"));
        personal.setLoginLogoPath("loginlogo.svg");
        personal.setLoginLogo(loadFile(loginIs,"loginlogo.svg"));
        personal.setTabLogo(loadFile(tabIs,"tablogo.svg"));
        personal.setTabLogoPath("tablogo.svg");
        personal.setTitle("Zeus");
        personal.setSlogan("让IT更美好");
        personal.setCopyrightNotice("Copyright © 2021 杭州谐云科技有限公司 All rights reserved.Copyright.");
        personal.setPlatformName("Zeus | 中间件管理一体化平台");
        personal.setPlatformAliasName("中间件一体化管理平台");
        personal.setCreateTime(new Date());
        personal.setStatus("0");
        personalMapper.insert(personal);
    }

    /**
     * 将文件转为二进制数组
     * @param is
     * @return
     * @throws IOException
     */
    private String  loadFile(InputStream is,String name) throws IOException {

        byte[] bus = null;
        byte[] by = null;
        try {
            if (is != null) {
                by = new byte[is.available()];
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                int n;
                //inPut.read(by)从(来源)输入流中(读取内容)读取的一定数量字节数,并将它们存储到(去处)缓冲区数组by中
                while ((n = is.read(by)) != -1) {
                    bos.write(by, 0, n);
                }
                bus = bos.toByteArray();
            }
        }catch (IOException e){
            e.printStackTrace();
        }finally {
            is.close();
        }
        String voiceBase64= Base64.getEncoder().encodeToString(bus);
        if (name.contains("svg")) {
            voiceBase64 = "data:image/svg+xml;base64," + voiceBase64;
        }
        if (name.contains("jpg") || name.contains("jpeg")) {
            voiceBase64 = "data:image/jpeg;base64," + voiceBase64;
        }
        if (name.contains("png")) {
            voiceBase64 = "data:image/png;base64," + voiceBase64;
        }
        return voiceBase64;
    }
}
