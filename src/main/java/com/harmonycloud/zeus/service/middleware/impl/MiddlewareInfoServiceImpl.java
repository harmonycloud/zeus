package com.harmonycloud.zeus.service.middleware.impl;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCRD;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.middleware.MiddlewareTypeEnum;
import com.harmonycloud.caas.common.model.middleware.Middleware;
import com.harmonycloud.caas.common.model.middleware.MiddlewareInfoDTO;
import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;
import com.harmonycloud.zeus.dao.BeanMiddlewareInfoMapper;
import com.harmonycloud.zeus.service.k8s.MiddlewareCRDService;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;

import lombok.extern.slf4j.Slf4j;

import static com.harmonycloud.caas.common.constants.CommonConstant.DOT;
import static com.harmonycloud.caas.common.constants.CommonConstant.LINE;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.HARMONY_CLOUD;
import static com.harmonycloud.caas.common.constants.registry.HelmChartConstant.ICON_SVG;
import static com.harmonycloud.caas.common.constants.registry.HelmChartConstant.SVG;

/**
 * @author dengyulong
 * @date 2021/03/23
 */
@Slf4j
@Service
public class MiddlewareInfoServiceImpl implements MiddlewareInfoService {

    @Value("${system.images.path:/usr/local/zeus-pv/images/middleware}")
    private String imagePath;

    @Autowired
    private BeanMiddlewareInfoMapper middlewareInfoMapper;
    @Autowired
    private MiddlewareCRDService middlewareCRDService;

    @Override
    public List<BeanMiddlewareInfo> list(String clusterId) {
        QueryWrapper<BeanMiddlewareInfo> wrapper = new QueryWrapper<BeanMiddlewareInfo>().eq("status", true);
        List<BeanMiddlewareInfo> list = middlewareInfoMapper.selectList(wrapper);
        if (list == null) {
            list = new ArrayList<>(0);
        }
        if (StringUtils.isEmpty(clusterId)){
            return list;
        }
        list.forEach(l -> {
            if (StringUtils.isEmpty(l.getClusterId())) {
                l.setClusterId(clusterId);
                middlewareInfoMapper.updateById(l);
            }
        });
        return list.stream().filter(l -> l.getClusterId().equals(clusterId)).collect(Collectors.toList());
    }

    @Override
    public BeanMiddlewareInfo getMiddlewareInfo(String chartName, String chartVersion) {
        QueryWrapper<BeanMiddlewareInfo> wrapper = new QueryWrapper<BeanMiddlewareInfo>().eq("status", true)
            .eq("chart_name", chartName).eq("chart_version", chartVersion);
        List<BeanMiddlewareInfo> mwInfoList = middlewareInfoMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(mwInfoList)){
            return null;
        }
        return mwInfoList.get(0);
    }

    @Override
    public List<MiddlewareInfoDTO> list(String clusterId, String namespace) {
        List<BeanMiddlewareInfo> list = list(clusterId);
        if (list.size() == 0) {
            return new ArrayList<>(0);
        }

        List<MiddlewareCRD> crdList = middlewareCRDService.listCRD(clusterId, namespace, null);
        Map<String, List<Middleware>> middlewareMap = null;
        if (!CollectionUtils.isEmpty(crdList)) {
            middlewareMap = crdList.stream()
                .collect(Collectors.groupingBy(crd -> MiddlewareTypeEnum.findTypeByCrdType(crd.getSpec().getType()),
                    Collectors.mapping(c -> middlewareCRDService.simpleConvert(c), Collectors.toList())));
        }
        Map<String, List<Middleware>> finalMiddlewareMap = middlewareMap;
        return list.stream().map(info -> {
            MiddlewareInfoDTO dto = new MiddlewareInfoDTO();
            BeanUtils.copyProperties(info, dto);
            dto.setMiddlewares(finalMiddlewareMap == null || finalMiddlewareMap.get(dto.getChartName()) == null
                ? new ArrayList<>(0) : finalMiddlewareMap.get(dto.getChartName()))
                .setReplicas(dto.getMiddlewares().size()).setReplicasStatus(
                    dto.getMiddlewares().stream().allMatch(m -> StringUtils.equals(m.getStatus(), "Running")));
            return dto;
        }).collect(Collectors.toList());
    }
    
    @Override
    public void update(BeanMiddlewareInfo middlewareInfo) {
        if (middlewareInfo.getId() != null) {
            middlewareInfoMapper.updateById(middlewareInfo);
        } else {
            QueryWrapper<BeanMiddlewareInfo> wrapper = new QueryWrapper<BeanMiddlewareInfo>()
                .eq("chart_name", middlewareInfo.getChartName()).eq("chart_version", middlewareInfo.getChartVersion());
            middlewareInfoMapper.update(middlewareInfo, wrapper);
        }
    }

    @Override
    public void insert(HelmChartFile helmChartFile, String path, String clusterId) {
        LambdaQueryWrapper<BeanMiddlewareInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BeanMiddlewareInfo::getChartName, helmChartFile.getChartName())
                .eq(BeanMiddlewareInfo::getChartVersion, helmChartFile.getChartVersion())
                .eq(BeanMiddlewareInfo::getClusterId, clusterId).orderByDesc(BeanMiddlewareInfo::getUpdateTime);
        List<BeanMiddlewareInfo> beanMiddlewareInfos = middlewareInfoMapper.selectList(queryWrapper);

        BeanMiddlewareInfo middlewareInfo = new BeanMiddlewareInfo();
        middlewareInfo.setName(helmChartFile.getChartName());
        middlewareInfo.setChartName(helmChartFile.getChartName());
        middlewareInfo.setType(helmChartFile.getType());
        middlewareInfo.setName(helmChartFile.getChartName());
        middlewareInfo.setDescription(helmChartFile.getDescription());
        middlewareInfo.setClusterId(clusterId);
        middlewareInfo.setOfficial(HARMONY_CLOUD.equals(helmChartFile.getOfficial()));
        LocalDateTime now = LocalDateTime.now();
        middlewareInfo.setUpdateTime(now);
        List<Path> iconFiles = searchFiles(path + File.separator + helmChartFile.getTarFileName(), ICON_SVG);
        if (!CollectionUtils.isEmpty(iconFiles)) {
            // 获取image路径
            String imagePath = helmChartFile.getChartName() + LINE + helmChartFile.getChartVersion() + DOT + SVG;
            middlewareInfo.setImagePath(imagePath);
            middlewareInfo.setImage(file2byte(iconFiles.get(0)));
            try {
                // 保存图片
                saveImg(iconFiles.get(0).toString(), imagePath);
            } catch (Exception e) {
                log.error("中间件{} 保存图片失败", middlewareInfo.getChartName());
            }
        }
        if (CollectionUtils.isEmpty(beanMiddlewareInfos)) {
            middlewareInfo.setChartVersion(helmChartFile.getChartVersion());
            middlewareInfo.setVersion(helmChartFile.getAppVersion());
            middlewareInfo.setCreateTime(now);
            middlewareInfoMapper.insert(middlewareInfo);
        } else {
            middlewareInfo.setId(beanMiddlewareInfos.get(0).getId());
            middlewareInfoMapper.updateById(middlewareInfo);
        }
    }

    /**
     * 采用搜索的方式查找文件。在chart当前目录搜索。
     * @param folderPath
     * @param fileName
     * @return
     */
    public static List<Path> searchFiles(String folderPath, final String fileName) {
        try {
            return Files.list(Paths.get(folderPath))
                    .filter(s -> isIconMatch(s, fileName)).collect(Collectors.toList());
        } catch (IOException e) {
            log.error("search file failed", e);
        }
        return Collections.EMPTY_LIST;
    }

    /**
     * chart图标名称匹配。如果没有指定文件名，则判断文件图片格式。
     * @param path
     * @param fileName
     * @return
     */
    private static boolean isIconMatch(Path path, final String fileName){
        return null == fileName
                ? path.getFileName().toString().matches(".+(?i)(.svg|.png)$")
                : StringUtils.equals(path.getFileName().toString(), fileName);
    }

    public void saveImg(String path, String name) throws IOException {
        File img = new File(path);
        File tarFile = new File(imagePath + File.separator + name);
        FileUtils.copyFile(img, tarFile);
    }

    /**
     * 将文件转换成byte数组
     * @param path
     * @return
     */
    private byte[] file2byte(Path path){
        try (FileChannel fc = new RandomAccessFile(path.toAbsolutePath().toString(), "r").getChannel()){
            MappedByteBuffer byteBuffer = fc.map(FileChannel.MapMode.READ_ONLY, 0,
                    fc.size()).load();
            byte[] result = new byte[(int) fc.size()];
            if (byteBuffer.remaining() > 0) {
                byteBuffer.get(result, 0, byteBuffer.remaining());
            }
            return result;
        } catch (IOException e) {
            log.error("transform to byte failed", e);
        }
        return null;
    }
}
