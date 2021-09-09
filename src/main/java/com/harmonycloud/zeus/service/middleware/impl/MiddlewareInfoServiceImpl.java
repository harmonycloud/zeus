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
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.zeus.bean.BeanClusterMiddlewareInfo;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCRD;
import com.harmonycloud.zeus.service.k8s.PodService;
import com.harmonycloud.zeus.service.middleware.ClusterMiddlewareInfoService;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareInfo;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.util.ServiceNameConvertUtil;
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
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.PODS;
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
    @Autowired
    private MiddlewareService middlewareService;
    @Autowired
    private ClusterMiddlewareInfoService clusterMiddlewareInfoService;
    @Autowired
    private PodService podService;

    @Override
    public List<BeanMiddlewareInfo> list() {
        QueryWrapper<BeanMiddlewareInfo> wrapper = new QueryWrapper<>();
        List<BeanMiddlewareInfo> list = middlewareInfoMapper.selectList(wrapper);
        Map<String, List<BeanMiddlewareInfo>> map =
            list.stream().collect(Collectors.groupingBy(BeanMiddlewareInfo::getChartName));
        List<BeanMiddlewareInfo> mwInfo = new ArrayList<>();
        for (String key : map.keySet()){
            map.get(key).sort(Comparator.comparing(BeanMiddlewareInfo::getChartVersion).reversed());
            mwInfo.add(map.get(key).get(0));
        }
        return mwInfo;
    }

    @Override
    public List<BeanMiddlewareInfo> listByType(String type) {
        QueryWrapper<BeanMiddlewareInfo> wrapper = new QueryWrapper<BeanMiddlewareInfo>().eq("chart_name", type);
        return middlewareInfoMapper.selectList(wrapper);
    }

    @Override
    public BeanMiddlewareInfo get(String chartName, String chartVersion) {
        QueryWrapper<BeanMiddlewareInfo> wrapper = new QueryWrapper<BeanMiddlewareInfo>().eq("chart_name", chartName).eq("chart_version", chartVersion);
        return middlewareInfoMapper.selectOne(wrapper);
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
    public List<MiddlewareInfoDTO> list(String clusterId) {
        //获取中间件列表
        QueryWrapper<BeanMiddlewareInfo> wrapper = new QueryWrapper<>();
        List<BeanMiddlewareInfo> mwInfoList = middlewareInfoMapper.selectList(wrapper);
        if (mwInfoList.size() == 0) {
            return new ArrayList<>(0);
        }

    @Override
    public List<MiddlewareInfoDTO> version(String clusterId, String type) {
        QueryWrapper<BeanMiddlewareInfo> wrapper = new QueryWrapper<BeanMiddlewareInfo>().eq("chart_name", type);
        List<BeanMiddlewareInfo> mwInfoList = middlewareInfoMapper.selectList(wrapper);
        //根据版本倒序排列
        mwInfoList.sort(Comparator.comparing(BeanMiddlewareInfo::getChartVersion).reversed());
        BeanClusterMiddlewareInfo clusterMwInfo = clusterMiddlewareInfoService.get(clusterId, type);
        return mwInfoList.stream().map(info -> {
            MiddlewareInfoDTO dto = new MiddlewareInfoDTO();
            BeanUtils.copyProperties(info, dto);
            if (info.getChartVersion().compareTo(clusterMwInfo.getChartVersion()) < 0) {
                dto.setVersionStatus("history");
            } else if (info.getChartVersion().compareTo(clusterMwInfo.getChartVersion()) == 0) {
                dto.setVersionStatus("now");
            } else {
                dto.setVersionStatus("future");
            }
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
    public void insert(HelmChartFile helmChartFile, File file, String clusterId) {
        LambdaQueryWrapper<BeanMiddlewareInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BeanMiddlewareInfo::getChartName, helmChartFile.getChartName())
                .eq(BeanMiddlewareInfo::getChartVersion, helmChartFile.getChartVersion())
                .orderByDesc(BeanMiddlewareInfo::getUpdateTime);
        List<BeanMiddlewareInfo> beanMiddlewareInfos = middlewareInfoMapper.selectList(queryWrapper);

        BeanMiddlewareInfo middlewareInfo = new BeanMiddlewareInfo();
        middlewareInfo.setName(helmChartFile.getChartName());
        middlewareInfo.setChartName(helmChartFile.getChartName());
        middlewareInfo.setType(helmChartFile.getType());
        middlewareInfo.setName(helmChartFile.getChartName());
        middlewareInfo.setDescription(helmChartFile.getDescription());
        middlewareInfo.setOfficial(HARMONY_CLOUD.equals(helmChartFile.getOfficial()));
        //LocalDateTime now = LocalDateTime.now();
        middlewareInfo.setUpdateTime(new Date());
        List<Path> iconFiles = searchFiles(file.getParent() + File.separator + helmChartFile.getTarFileName(), ICON_SVG);
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
        //将helm chart包存入数据库
        middlewareInfo.setChart(file2byte(Paths.get(file.getAbsolutePath())));
        if (CollectionUtils.isEmpty(beanMiddlewareInfos)) {
            middlewareInfo.setChartVersion(helmChartFile.getChartVersion());
            middlewareInfo.setVersion(helmChartFile.getAppVersion());
            middlewareInfo.setCreateTime(new Date());
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

    public void saveStatus(Map<String, BeanClusterMiddlewareInfo> clusterMwInfoMap) {
        for (String key : clusterMwInfoMap.keySet()) {
            clusterMiddlewareInfoService.update(clusterMwInfoMap.get(key));
        }
    }



    @Override
    public List listAllMiddleware(String clusterId, String namespace) {
        List<MiddlewareInfoDTO> middlewareInfoDTOList = list(clusterId, namespace);
        List<Map<String, Object>> serviceList = new ArrayList<>();
        middlewareInfoDTOList.forEach(middlewareInfoDTO -> {
            List<Middleware> middlewareServiceList = middlewareService.simpleList(clusterId, namespace, middlewareInfoDTO.getChartName(), null);
            middlewareServiceList.forEach(middleware -> {
                MiddlewareCRD middlewareCRD = middlewareCRDService.getCR(clusterId, namespace, middlewareInfoDTO.getType(), middleware.getName());
                if (middlewareCRD != null) {
                    List<MiddlewareInfo> middlewareInfos = middlewareCRD.getStatus().getInclude().get(PODS);
                    middleware.setPodNum(middlewareInfos.size());
                    if (middleware.getManagePlatform() != null && middleware.getManagePlatform()) {
                        String managePlatformAddress = middlewareService.getManagePlatformAddress(middleware, clusterId);
                        middleware.setManagePlatformAddress(managePlatformAddress);
                    }
                }
            });

            Map<String, Object> middlewareMap = new HashMap<>();
            middlewareMap.put("name", middlewareInfoDTO.getChartName());
            middlewareMap.put("image", middlewareInfoDTO.getImage());
            middlewareMap.put("imagePath", middlewareInfoDTO.getImagePath());
            middlewareMap.put("serviceList", middlewareServiceList);
            middlewareMap.put("serviceNum", middlewareServiceList.size());
            serviceList.add(middlewareMap);
        });
        Collections.sort(serviceList, new ServiceMapComparator());

        return serviceList;
    }

    /**
     * 服务排序类，按服务数量进行排序
     */
    public static class ServiceMapComparator implements Comparator<Map> {
        @Override
        public int compare(Map service1, Map service2) {
            if (Integer.parseInt(service1.get("serviceNum").toString()) > Integer.parseInt(service2.get("serviceNum").toString())) {
                return -1;
            } else {
                return 1;
            }
        }
    }
}
