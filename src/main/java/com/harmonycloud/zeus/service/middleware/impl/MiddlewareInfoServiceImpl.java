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
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.harmonycloud.caas.common.model.middleware.PodInfo;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.zeus.bean.BeanClusterMiddlewareInfo;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareCRD;
import com.harmonycloud.zeus.integration.cluster.bean.MiddlewareInfo;
import com.harmonycloud.zeus.service.k8s.PodService;
import com.harmonycloud.zeus.service.middleware.ClusterMiddlewareInfoService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
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
    private ClusterMiddlewareInfoService clusterMiddlewareInfoService;
    @Autowired
    private PodService podService;
    @Autowired
    private MiddlewareCRDService middlewareCRDService;
    @Autowired
    private MiddlewareService middlewareService;

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
        Map<String, List<BeanMiddlewareInfo>> mwInfoMap =
                mwInfoList.stream().collect(Collectors.groupingBy(BeanMiddlewareInfo::getChartName));
        //获取集群中间件关联信息
        List<BeanClusterMiddlewareInfo> clusterMwInfoList = clusterMiddlewareInfoService.list(clusterId);
        //校验数据完整性
        if (CollectionUtils.isEmpty(clusterMwInfoList) || clusterMwInfoList.size() < mwInfoMap.size()) {
            for (String key : mwInfoMap.keySet()) {
                if (clusterMwInfoList.stream().noneMatch(
                        clusterMwInfo -> clusterMwInfo.getChartName().equals(mwInfoMap.get(key).get(0).getChartName()))) {
                    //获取最新版本的中间件写入集群中间件关联关系
                    List<BeanMiddlewareInfo> list = mwInfoMap.get(key);
                    list.sort(Comparator.comparing(BeanMiddlewareInfo::getChartVersion).reversed());
                    BeanClusterMiddlewareInfo clusterMwInfo = new BeanClusterMiddlewareInfo();
                    clusterMwInfo.setClusterId(clusterId);
                    clusterMwInfo.setChartName(list.get(0).getChartName());
                    clusterMwInfo.setChartVersion(list.get(0).getChartVersion());
                    clusterMwInfo.setStatus(2);
                    clusterMiddlewareInfoService.insert(clusterMwInfo);
                    clusterMwInfoList.add(clusterMwInfo);
                }
            }
        }
        //过滤中间件
        mwInfoList =
                mwInfoList.stream()
                        .filter(mwInfo -> clusterMwInfoList.stream()
                                .anyMatch(clusterMwInfo -> clusterMwInfo.getChartName().equals(mwInfo.getChartName())
                                        && clusterMwInfo.getChartVersion().equals(mwInfo.getChartVersion())))
                        .collect(Collectors.toList());
        Map<String, BeanClusterMiddlewareInfo> clusterMwInfoMap = clusterMwInfoList.stream()
                .collect(Collectors.toMap(info -> info.getChartName() + "-" + info.getChartVersion(), info -> info));
        //0-创建中 1-创建成功  2-待安装  3-运行异常
        List<PodInfo> podList = podService.list(clusterId, "middleware-operator");
        podList = podList.stream().filter(pod -> pod.getPodName().contains("operator")).collect(Collectors.toList());
        // 转化为map，并去除pod name 后缀中的随机码
        Map<String, List<PodInfo>> podMap = podList.stream().collect(Collectors.groupingBy(info -> info.getPodName()
            .substring(0, info.getPodName().lastIndexOf("-", info.getPodName().lastIndexOf("-") - 1))));
        mwInfoList.forEach(mwInfo -> {
            String key = mwInfo.getChartName() + "-" + mwInfo.getChartVersion();
            if (podMap.containsKey(mwInfo.getOperatorName())) {
                List<PodInfo> podInfoList = podMap.get(mwInfo.getOperatorName());
                if (podInfoList.size() == 1 && "Running".equals(podMap.get(mwInfo.getOperatorName()).get(0).getStatus())) {
                    clusterMwInfoMap.get(key).setStatus(1);
                } else if (clusterMwInfoMap.get(key).getStatus() != 0) {
                    clusterMwInfoMap.get(key).setStatus(3);
                }
            } else {
                clusterMwInfoMap.get(key).setStatus(2);
            }
        });
        //保存状态
        saveStatus(clusterMwInfoMap);
        return mwInfoList.stream().map(info -> {
            MiddlewareInfoDTO dto = new MiddlewareInfoDTO();
            BeanUtils.copyProperties(info, dto);
            dto.setStatus(clusterMwInfoMap.get(info.getChartName() + "-" + info.getChartVersion()).getStatus());
            return dto;
        }).collect(Collectors.toList());
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
    public void insert(HelmChartFile helmChartFile, File file) {
        LambdaQueryWrapper<BeanMiddlewareInfo> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(BeanMiddlewareInfo::getChartName, helmChartFile.getChartName())
                .eq(BeanMiddlewareInfo::getChartVersion, helmChartFile.getChartVersion());
        List<BeanMiddlewareInfo> beanMiddlewareInfos = middlewareInfoMapper.selectList(queryWrapper);

        BeanMiddlewareInfo middlewareInfo = new BeanMiddlewareInfo();
        middlewareInfo.setName(helmChartFile.getChartName());
        middlewareInfo.setChartName(helmChartFile.getChartName());
        middlewareInfo.setType(helmChartFile.getType());
        middlewareInfo.setName(helmChartFile.getChartName());
        middlewareInfo.setDescription(helmChartFile.getDescription());
        middlewareInfo.setOperatorName(helmChartFile.getDependency().get("alias"));
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
    public List listAllMiddleware(String clusterId, String namespace,String keyword) {
        List<MiddlewareInfoDTO> middlewareInfoDTOList = list(clusterId);
        List<Map<String, Object>> serviceList = new ArrayList<>();
        middlewareInfoDTOList.forEach(middlewareInfoDTO -> {
            List<Middleware> middlewareServiceList = middlewareService.simpleList(clusterId, namespace, middlewareInfoDTO.getChartName(), keyword);
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
            middlewareMap.put("chartName", middlewareInfoDTO.getChartName());
            middlewareMap.put("chartVersion", middlewareInfoDTO.getChartVersion());
            middlewareMap.put("version", middlewareInfoDTO.getVersion());
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
