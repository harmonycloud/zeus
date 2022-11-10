package com.harmonycloud.zeus.service.middleware.impl;

import static com.harmonycloud.caas.common.constants.CommonConstant.*;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.HARMONY_CLOUD;
import static com.harmonycloud.caas.common.constants.middleware.MiddlewareConstant.MIDDLEWARE_OPERATOR;
import static com.harmonycloud.caas.common.constants.registry.HelmChartConstant.ICON_SVG;
import static com.harmonycloud.caas.common.constants.registry.HelmChartConstant.SVG;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.harmonycloud.caas.common.enums.middleware.MiddlewareOfficialNameEnum;
import com.harmonycloud.caas.common.model.middleware.*;
import com.harmonycloud.zeus.bean.BeanMiddlewareCluster;
import com.harmonycloud.zeus.integration.registry.bean.harbor.HelmListInfo;
import com.harmonycloud.zeus.service.k8s.ClusterService;
import com.harmonycloud.zeus.service.k8s.MiddlewareClusterService;
import com.harmonycloud.zeus.service.middleware.MiddlewareService;
import com.harmonycloud.zeus.service.registry.HelmChartService;
import com.harmonycloud.zeus.util.ChartVersionUtil;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.registry.HelmChartFile;
import com.harmonycloud.zeus.bean.BeanClusterMiddlewareInfo;
import com.harmonycloud.zeus.bean.BeanMiddlewareInfo;
import com.harmonycloud.zeus.dao.BeanMiddlewareInfoMapper;
import com.harmonycloud.zeus.service.k8s.PodService;
import com.harmonycloud.zeus.service.middleware.ClusterMiddlewareInfoService;
import com.harmonycloud.zeus.service.middleware.MiddlewareInfoService;
import com.harmonycloud.zeus.util.MathUtil;

import lombok.extern.slf4j.Slf4j;

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
    private HelmChartService helmChartService;
    @Autowired
    private ClusterService clusterService;
    @Autowired
    private MiddlewareClusterService middlewareClusterService;
    @Autowired
    private MiddlewareService middlewareService;

    @Override
    public List<BeanMiddlewareInfo> list(Boolean all) {
        QueryWrapper<BeanMiddlewareInfo> wrapper = new QueryWrapper<BeanMiddlewareInfo>().select("id", "name",
            "description", "type", "version", "image_path", "chart_name", "chart_version", "operator_name",
            "grafana_id", "create_time", "update_time", "official", "compatible_versions");
        List<BeanMiddlewareInfo> list = middlewareInfoMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(list)) {
            return new ArrayList<>();
        }
        if (!all) {
            return filter(list);
        }
        return list;
    }

    @Override
    public List<BeanMiddlewareInfo> listByType(String type) {
        QueryWrapper<BeanMiddlewareInfo> wrapper = new QueryWrapper<BeanMiddlewareInfo>().eq("chart_name", type);
        List<BeanMiddlewareInfo> middlewareInfoList = middlewareInfoMapper.selectList(wrapper);
        compareChartVersion(middlewareInfoList);
        return middlewareInfoList;
    }

    @Override
    public BeanMiddlewareInfo get(String chartName, String chartVersion) {
        QueryWrapper<BeanMiddlewareInfo> wrapper =
            new QueryWrapper<BeanMiddlewareInfo>().eq("chart_name", chartName).eq("chart_version", chartVersion);
        List<BeanMiddlewareInfo> mwInfoList = middlewareInfoMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(mwInfoList)) {
            return null;
        }
        return mwInfoList.get(0);
    }

    @Override
    public List<MiddlewareInfoDTO> list(String clusterId) {
        //获取中间件列表
        List<BeanMiddlewareInfo> mwInfoList = this.list(true);
        if (mwInfoList.size() == 0) {
            return new ArrayList<>(0);
        }
        Map<String, List<BeanMiddlewareInfo>> mwInfoMap =
                mwInfoList.stream().collect(Collectors.groupingBy(BeanMiddlewareInfo::getChartName));
        if (StringUtils.isEmpty(clusterId)) {
            return simpleList(mwInfoMap);
        }
        //获取集群中间件关联信息
        List<BeanClusterMiddlewareInfo> clusterMwInfoList;
        synchronized (this){
            clusterMwInfoList = clusterMiddlewareInfoService.list(clusterId);
            //校验数据完整性
            checkDate(clusterMwInfoList, mwInfoMap, clusterId);
        }
        //过滤中间件
        mwInfoList =
                mwInfoList.stream()
                        .filter(mwInfo -> clusterMwInfoList.stream()
                                .anyMatch(clusterMwInfo -> clusterMwInfo.getChartName().equals(mwInfo.getChartName())
                                        && clusterMwInfo.getChartVersion().equals(mwInfo.getChartVersion())))
                        .collect(Collectors.toList());
        List<BeanClusterMiddlewareInfoDTO> clusterMwInfoDtoList = clusterMwInfoList.stream().map(clusterMwInfo -> {
            BeanClusterMiddlewareInfoDTO beanClusterMiddlewareInfoDTO = new BeanClusterMiddlewareInfoDTO();
            BeanUtils.copyProperties(clusterMwInfo,beanClusterMiddlewareInfoDTO);
            return beanClusterMiddlewareInfoDTO;
        }).collect(Collectors.toList());
        Map<String, BeanClusterMiddlewareInfoDTO> clusterMwInfoDtoMap = clusterMwInfoDtoList.stream()
                .collect(Collectors.toMap(info -> info.getChartName() + "-" + info.getChartVersion(), info -> info));
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
            if (StringUtils.isEmpty(mwInfo.getOperatorName())){
                return;
            }
            if (podMap.containsKey(mwInfo.getOperatorName())) {
                //过滤被驱逐的pod
                List<PodInfo> podInfoList = podMap.get(mwInfo.getOperatorName()).stream()
                    .filter(podInfo -> !"Evicted".equals(podInfo.getStatus())).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(podInfoList)) {
                    clusterMwInfoDtoMap.get(key).setReplicas(podInfoList.size());
                }
                if (podInfoList.stream().allMatch(podInfo -> "Running".equals(podInfo.getStatus()))) {
                    clusterMwInfoMap.get(key).setStatus(1);
                    clusterMwInfoDtoMap.get(key).setStatus(1);
                } else if (clusterMwInfoMap.get(key).getStatus() != 0) {
                    clusterMwInfoMap.get(key).setStatus(3);
                    clusterMwInfoDtoMap.get(key).setStatus(3);
                }
            } else {
                clusterMwInfoMap.get(key).setStatus(2);
                clusterMwInfoDtoMap.get(key).setStatus(2);
            }
        });
        //保存状态
        saveStatus(clusterMwInfoMap);
        return mwInfoList.stream().map(info -> {
            MiddlewareInfoDTO dto = new MiddlewareInfoDTO();
            BeanUtils.copyProperties(info, dto);
            dto.setName(MiddlewareOfficialNameEnum.findByChartName(dto.getName()));
            dto.setStatus(clusterMwInfoDtoMap.get(info.getChartName() + "-" + info.getChartVersion()).getStatus());
            dto.setReplicas(clusterMwInfoDtoMap.get(info.getChartName() + "-" + info.getChartVersion()).getReplicas());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public MiddlewareInfoDTO getByType(String clusterId, String type) {
        List<BeanMiddlewareInfo> beanMiddlewareInfoList = listByType(type);
        if (CollectionUtils.isEmpty(beanMiddlewareInfoList)) {
            return null;
        }
        BeanClusterMiddlewareInfo beanClusterMiddlewareInfo = clusterMiddlewareInfoService.get(clusterId, type);
        beanMiddlewareInfoList = beanMiddlewareInfoList.stream()
            .filter(mwInfo -> mwInfo.getChartVersion().equals(beanClusterMiddlewareInfo.getChartVersion()))
            .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(beanMiddlewareInfoList)) {
            return null;
        }
        MiddlewareInfoDTO middlewareInfoDTO = new MiddlewareInfoDTO();
        BeanUtils.copyProperties(beanMiddlewareInfoList.get(0), middlewareInfoDTO);
        return middlewareInfoDTO;
    }

    @Override
    public List<MiddlewareInfoDTO> version(String clusterId, String type) {
        List<BeanMiddlewareInfo> mwInfoList = listByType(type);
        BeanClusterMiddlewareInfo clusterMwInfo = clusterMiddlewareInfoService.get(clusterId, type);
        if (mwInfoList.size() == 1 && (clusterMwInfo == null || clusterMwInfo.getStatus() == 2)) {
            return mwInfoList.stream().map(info -> {
                MiddlewareInfoDTO dto = new MiddlewareInfoDTO();
                BeanUtils.copyProperties(info, dto);
                return dto.setVersionStatus("future");
            }).collect(Collectors.toList());
        }
        return mwInfoList.stream().map(info -> {
            MiddlewareInfoDTO dto = new MiddlewareInfoDTO();
            BeanUtils.copyProperties(info, dto);
            if (ChartVersionUtil.compare(clusterMwInfo.getChartVersion(), info.getChartVersion()) < NUM_ZERO) {
                dto.setVersionStatus("history");
            } else if (info.getChartVersion().compareTo(clusterMwInfo.getChartVersion()) == NUM_ZERO) {
                int status = clusterMwInfo.getStatus();
                switch (status) {
                    case NUM_ZERO:
                        dto.setVersionStatus("updating");
                        break;
                    case NUM_TWO:
                        dto.setVersionStatus("future");
                        break;
                    default:
                        dto.setVersionStatus("now");
                }
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
        middlewareInfo.setCompatibleVersions(helmChartFile.getCompatibleVersions());
        if(!CollectionUtils.isEmpty(helmChartFile.getDependency())){
            middlewareInfo.setOperatorName(helmChartFile.getDependency().get("alias"));
        }
        middlewareInfo.setOfficial(HARMONY_CLOUD.equals(helmChartFile.getOfficial()));
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

    @Override
    public void delete(String chartName, String chartVersion) {
        // 查看此版本是否被某集群安装中
        List<BeanClusterMiddlewareInfo> clusterMwInfoList =
            clusterMiddlewareInfoService.listByChart(chartName, chartVersion);
        if (clusterMwInfoList.stream().anyMatch(clusterMwInfo -> !clusterMwInfo.getStatus().equals(2))) {
            throw new BusinessException(ErrorMessage.MIDDLEWARE_STILL_BE_USED);
        }
        // 删除中间件信息
        QueryWrapper<BeanMiddlewareInfo> wrapper =
            new QueryWrapper<BeanMiddlewareInfo>().eq("chart_name", chartName).eq("chart_version", chartVersion);
        middlewareInfoMapper.delete(wrapper);
        // 删除集群绑定信息
        clusterMwInfoList.forEach(clusterMwInfo -> clusterMiddlewareInfoService.delete(clusterMwInfo.getClusterId(),
            clusterMwInfo.getChartName(), clusterMwInfo.getChartVersion()));
    }

    @Override
    public List<BeanMiddlewareInfo> filter(List<BeanMiddlewareInfo> beanMiddlewareInfoList) {
        Map<String, List<BeanMiddlewareInfo>> map =
                beanMiddlewareInfoList.stream().collect(Collectors.groupingBy(BeanMiddlewareInfo::getChartName));
        List<BeanMiddlewareInfo> mwInfoList = new ArrayList<>();
        for (String key : map.keySet()) {
            compareChartVersion(map.get(key));
            mwInfoList.add(map.get(key).get(0));
        }
        return mwInfoList;
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
    public MiddlewareOperatorDTO getOperatorInfo(List<MiddlewareClusterDTO> clusterList){
        MiddlewareOperatorDTO operatorDTO = new MiddlewareOperatorDTO();
        List<MiddlewareOperatorDTO.MiddlewareOperator> operatorList = new ArrayList<>();
        AtomicInteger runningOperator = new AtomicInteger();
        AtomicInteger errorOperator = new AtomicInteger();

        for (MiddlewareClusterDTO clusterDTO : clusterList) {
            List<MiddlewareInfoDTO> middlewareInfoDTOS = list(clusterDTO.getId());
            for (MiddlewareInfoDTO middlewareInfoDTO : middlewareInfoDTOS) {
                MiddlewareOperatorDTO.MiddlewareOperator operator = new MiddlewareOperatorDTO.MiddlewareOperator();
                if (middlewareInfoDTO.getStatus() == 2) {
                    continue;
                }
                operator.setName(middlewareInfoDTO.getName());
                operator.setClusterId(clusterDTO.getId());
                operator.setClusterName(clusterDTO.getNickname());
                operator.setStatus(middlewareInfoDTO.getStatus());
                if (middlewareInfoDTO.getStatus() == 1) {
                    runningOperator.getAndAdd(1);
                } else {
                    errorOperator.getAndAdd(1);
                }
                operatorList.add(operator);
            }
        }

        operatorDTO.setError(errorOperator.get());
        operatorDTO.setRunning(runningOperator.get());
        operatorDTO.setOperatorList(operatorList);
        operatorDTO.setTotal(operatorList.size());
        operatorDTO.setErrorPercent(MathUtil.calcPercent(errorOperator.get(), operatorList.size()));
        operatorDTO.setRunningPercent(MathUtil.calcPercent(runningOperator.get(), operatorList.size()));
        return operatorDTO;
    }

    @Override
    public List<Middleware> middlewareList(String type, String keyword) {
        List<BeanMiddlewareCluster> clusterList = middlewareClusterService.listClustersByClusterId(null);
        List<Middleware> middlewareList = new ArrayList<>();
        clusterList.forEach(cluster -> {
            List<Namespace> listRegisteredNamespace = clusterService.listRegisteredNamespace(cluster.getClusterId());
            List<Middleware> middlewares;
            try{
                middlewares = middlewareService.simpleList(cluster.getClusterId(), null, null, null);
            }catch (Exception e){
                log.error("集群{}, 查询middleware失败", cluster.getClusterId());
                return;
            }
            middlewares = middlewares.stream().filter(middleware -> listRegisteredNamespace.stream().anyMatch(ns ->middleware.getNamespace().equals(ns.getName()))).collect(Collectors.toList());
            if (!middlewares.isEmpty()) {
                middlewares = checkIsLvm(middlewares);
            }
            if (middlewares.isEmpty()) {
                return;
            }
            if (StringUtils.isEmpty(keyword) && StringUtils.isEmpty(type)) {
                middlewareList.addAll(middlewares);
            } else {
                if (StringUtils.isNotEmpty(keyword)) {
                    middlewareList.addAll(middlewares.stream()
                            .filter(middleware -> middleware.getType().equals(type) && middleware.getName().contains(keyword))
                            .collect(Collectors.toList()));
                } else {
                    middlewareList.addAll(middlewares.stream().filter(middleware -> middleware.getType().equals(type))
                            .collect(Collectors.toList()));
                }
            }
        });
        return middlewareList;
    }

    @Override
    public List<MiddlewareInfoDTO> clusterList() {
        List<BeanMiddlewareCluster> clusterList = middlewareClusterService.listClustersByClusterId(null);
        List<MiddlewareInfoDTO> middlewareInfoDTOS = new ArrayList<>();
        clusterList.forEach(cluster -> {
            middlewareInfoDTOS.addAll(list(cluster.getClusterId()));
        });
        HashSet set = new HashSet(middlewareInfoDTOS);
        middlewareInfoDTOS.clear();
        middlewareInfoDTOS.addAll(set);
        return middlewareInfoDTOS;
    }

    @Override
    public List<BeanMiddlewareInfo> listInstalledByClusters(List<MiddlewareClusterDTO> clusterList) {
        return middlewareInfoMapper.listInstalledWithMiddlewareDetail(clusterList);
    }

    @Override
    public List<BeanMiddlewareInfo> listInstalledByCluster(MiddlewareClusterDTO clusterDTO) {
        List<MiddlewareClusterDTO> clusterDTOS = new ArrayList<>();
        clusterDTOS.add(clusterDTO);
        return middlewareInfoMapper.listInstalledWithMiddlewareDetail(clusterDTOS);
    }

    public void compareChartVersion(List<BeanMiddlewareInfo> mwInfoList){
        mwInfoList.sort((o1, o2) -> ChartVersionUtil.compare(o1.getChartVersion(), o2.getChartVersion()));
    }

    public List<MiddlewareInfoDTO> simpleList(Map<String, List<BeanMiddlewareInfo>> mwInfoMap){
        List<MiddlewareInfoDTO> list = new ArrayList<>();
        for (String key : mwInfoMap.keySet()) {
            MiddlewareInfoDTO middlewareInfoDTO = new MiddlewareInfoDTO().setChartName(key)
                    .setName(MiddlewareOfficialNameEnum.findByChartName(key));
            list.add(middlewareInfoDTO);
        }
        return list;
    }


    /**
     * 校验存储类型是否为LVM
     */
    private List<Middleware> checkIsLvm(List<Middleware> middlewareList) {
        List<Middleware> middlewares = new LinkedList<>();
        for (Middleware middleware : middlewareList) {
            if (middleware.getQuota() == null){
                continue;
            }
            switch (middleware.getType()) {
                case "redis":
                    if (middleware.getQuota().get("redis").getIsLvmStorage()) {
                        middlewares.add(middleware);
                    }
                    break;
                case "mysql":
                    middlewares.add(middleware);
                    break;
                case "rocketmq":
                    if (middleware.getQuota().get("rocketmq").getIsLvmStorage()) {
                        middlewares.add(middleware);
                    }
                    break;
                case "elasticsearch":
                    if (middleware.getQuota().get("master").getIsLvmStorage()) {
                        middlewares.add(middleware);
                    }
                    break;
                case "postgresql":
                    middlewares.add(middleware);
                    break;
            }
        }
        return middlewares;
    }

    /**
     * 数据校验
     */
    public void checkDate(List<BeanClusterMiddlewareInfo> clusterMwInfoList,
        Map<String, List<BeanMiddlewareInfo>> mwInfoMap, String clusterId) {
        // 判断存在数据缺失
        if (CollectionUtils.isEmpty(clusterMwInfoList) || clusterMwInfoList.size() < mwInfoMap.size()) {
            // 查寻是否存在已安装的operator
            Map<String, String> alreadyInstalledOperator = findAlreadyInstalledOperator(clusterId, mwInfoMap);
            for (String key : mwInfoMap.keySet()) {
                // 匹配缺失类型
                if (clusterMwInfoList.stream().noneMatch(
                    clusterMwInfo -> clusterMwInfo.getChartName().equals(mwInfoMap.get(key).get(0).getChartName()))) {
                    // 获取最新版本的中间件写入集群中间件关联关系
                    List<BeanMiddlewareInfo> list = mwInfoMap.get(key);
                    compareChartVersion(list);
                    BeanClusterMiddlewareInfo clusterMwInfo = new BeanClusterMiddlewareInfo();
                    clusterMwInfo.setClusterId(clusterId);
                    clusterMwInfo.setChartName(key);
                    if (alreadyInstalledOperator.containsKey(key)) {
                        clusterMwInfo.setChartVersion(alreadyInstalledOperator.get(key));
                    } else {
                        clusterMwInfo.setChartVersion(list.get(0).getChartVersion());
                    }
                    clusterMwInfo.setStatus(2);
                    clusterMiddlewareInfoService.insert(clusterMwInfo);
                    clusterMwInfoList.add(clusterMwInfo);
                }
            }
        }
    }

    /**
     * 获取环境中当前的operator版本
     */
    public Map<String, String> findAlreadyInstalledOperator(String clusterId,
        Map<String, List<BeanMiddlewareInfo>> mwInfoMap) {
        List<HelmListInfo> helmInfoList =
            helmChartService.listHelm(MIDDLEWARE_OPERATOR, null, clusterService.findById(clusterId));
        Map<String, String> helmInfoMap =
            helmInfoList.stream().collect(Collectors.toMap(HelmListInfo::getName, HelmListInfo::getChart));
        Map<String, String> operatorNameMap = getOperatorName(mwInfoMap);

        Map<String, String> res = new HashMap<>();
        operatorNameMap.forEach((k, v) -> {
            if (helmInfoMap.containsKey(v)) {
                String chartVersion = helmInfoMap.get(v).replace(v + "-", "");
                res.put(k, chartVersion);
            }
        });
        return res;
    }

    /**
     * 获取operatorname
     */
    public Map<String, String> getOperatorName(Map<String, List<BeanMiddlewareInfo>> mwInfoMap){
        Map<String, String> operatorMap = new HashMap<>();
        mwInfoMap.forEach((k, v) -> {
            operatorMap.put(k, mwInfoMap.get(k).get(0).getOperatorName());
        });
        return operatorMap;
    }
}
