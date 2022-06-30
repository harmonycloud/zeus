package com.harmonycloud.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.constants.CommonConstant;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.MiddlewareBackupRecord;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterBackupAddressDTO;
import com.harmonycloud.tool.uuid.UUIDUtils;
import com.harmonycloud.zeus.bean.BeanMiddlewareBackupAddress;
import com.harmonycloud.zeus.bean.BeanMiddlewareBackupToCluster;
import com.harmonycloud.zeus.bean.BeanMiddlewareCluster;
import com.harmonycloud.zeus.config.MyLambdaUpdateWrapper;
import com.harmonycloud.zeus.dao.BeanBackupAddressClusterMapper;
import com.harmonycloud.zeus.dao.BeanMiddlewareBackupAddressMapper;
import com.harmonycloud.zeus.integration.cluster.bean.Minio;
import com.harmonycloud.zeus.integration.minio.MinioWrapper;
import com.harmonycloud.zeus.service.k8s.MiddlewareClusterService;
import com.harmonycloud.zeus.service.middleware.MiddlewareBackupAddressService;
import com.harmonycloud.zeus.service.middleware.MiddlewareBackupService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yushuaikang
 * @Date 2022/6/9 14:55 下午
 */
@Slf4j
@Service
public class MiddlewareBackupAddressServiceImpl implements MiddlewareBackupAddressService {

    @Autowired
    private BeanMiddlewareBackupAddressMapper middlewareBackupAddressMapper;
    @Autowired
    private MinioWrapper minioWrapper;
    @Autowired
    private BeanBackupAddressClusterMapper backupAddressClusterMapper;
    @Autowired
    @Qualifier("middlewareBackupServiceImpl")
    private MiddlewareBackupService middlewareBackupService;
    @Autowired
    private MiddlewareClusterService middlewareClusterService;

    @Override
    public void createBackupAddress(MiddlewareClusterBackupAddressDTO middlewareClusterBackupAddressDTO) {
        QueryWrapper<BeanMiddlewareBackupAddress> wrapper = new QueryWrapper<BeanMiddlewareBackupAddress>().eq("name", middlewareClusterBackupAddressDTO.getName());
        List<BeanMiddlewareBackupAddress> backupAddressList = middlewareBackupAddressMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(backupAddressList)) {
             throw new BusinessException(ErrorMessage.CHINESE_NAME_REPETITION);
        }
        BeanMiddlewareBackupAddress backup = new BeanMiddlewareBackupAddress();
        BeanUtils.copyProperties(middlewareClusterBackupAddressDTO, backup);
        backup.setCreateTime(new Date());
        backup.setAddressId(UUIDUtils.get16UUID());
        middlewareBackupAddressMapper.insert(backup);
        middlewareClusterBackupAddressDTO.getClusterIds().forEach(cluster -> {
            BeanMiddlewareBackupToCluster backupToCluster = new BeanMiddlewareBackupToCluster();
            backupToCluster.setBackupAddressId(backup.getId());
            backupToCluster.setClusterId(cluster);
            backupAddressClusterMapper.insert(backupToCluster);
        });
    }

    @Override
    public void updateBackupAddress(MiddlewareClusterBackupAddressDTO middlewareClusterBackupAddressDTO) {
        BeanMiddlewareBackupAddress backup = new BeanMiddlewareBackupAddress();
        BeanUtils.copyProperties(middlewareClusterBackupAddressDTO, backup);
        middlewareBackupAddressMapper.updateById(backup);
        backupAddressClusterMapper
            .delete(new QueryWrapper<BeanMiddlewareBackupToCluster>().eq("backup_address_id", backup.getId()));
        middlewareClusterBackupAddressDTO.getClusterIds().forEach(cluster -> {
            BeanMiddlewareBackupToCluster backupToCluster = new BeanMiddlewareBackupToCluster();
            backupToCluster.setBackupAddressId(backup.getId());
            backupToCluster.setClusterId(cluster);
            backupAddressClusterMapper.insert(backupToCluster);
        });
    }

    @Override
    public List<MiddlewareClusterBackupAddressDTO> listBackupAddress(String addressId, String keyWord) {
        QueryWrapper<BeanMiddlewareBackupAddress> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(addressId)) {
            wrapper.eq("address_id", addressId);
        }
        List<BeanMiddlewareBackupAddress> backups = middlewareBackupAddressMapper.selectList(wrapper);
        if (CollectionUtils.isEmpty(backups)) {
            return new ArrayList<MiddlewareClusterBackupAddressDTO>();
        }
        List<MiddlewareClusterBackupAddressDTO> backupAddressDTOS = backups.stream().map(backup -> {
            MiddlewareClusterBackupAddressDTO backupDTO = new MiddlewareClusterBackupAddressDTO();
            BeanUtils.copyProperties(backup, backupDTO);
            List<String> clusterIds = new LinkedList<>();
            List<BeanMiddlewareBackupToCluster> backupToClusters = backupAddressClusterMapper
                    .selectList(new QueryWrapper<BeanMiddlewareBackupToCluster>().eq("backup_address_id", backup.getId()));
            backupToClusters.forEach(backupToCluster -> {
                clusterIds.add(backupToCluster.getClusterId());
            });
            backupDTO.setClusterIds(clusterIds);
            backupDTO.setRelevanceNum(calRelevanceNum(backupDTO.getName()));
            return backupDTO;
        }).collect(Collectors.toList());
        if (StringUtils.isNotEmpty(keyWord)) {
            return backupAddressDTOS.stream()
                    .filter(
                            middlewareClusterBackupAddressDTO -> middlewareClusterBackupAddressDTO.getName().contains(keyWord))
                    .collect(Collectors.toList());
        }
        return backupAddressDTOS;
    }

    private int calRelevanceNum(String addressName) {
        int relevanceNum = 0;
        List<BeanMiddlewareCluster> clusterList = middlewareClusterService.listClustersByClusterId(null);
        for (BeanMiddlewareCluster cluster : clusterList) {
            List<MiddlewareBackupRecord> backups = middlewareBackupService.backupTaskList(cluster.getClusterId(), "*", null, null, null);
            backups.stream().filter(backup -> addressName.equals(backup.getAddressName())).collect(Collectors.toList());
            relevanceNum = relevanceNum + backups.size();
        }
       return relevanceNum;
    }

    @Override
    public MiddlewareClusterBackupAddressDTO detail(Integer id) {
        MiddlewareClusterBackupAddressDTO backupDTO = new MiddlewareClusterBackupAddressDTO();

        BeanMiddlewareBackupAddress backupAddress = middlewareBackupAddressMapper.selectById(id);
        BeanUtils.copyProperties(backupAddress, backupDTO);
        List<String> clusterIds = new LinkedList<>();
        List<BeanMiddlewareBackupToCluster> backupToClusters = backupAddressClusterMapper
                .selectList(new QueryWrapper<BeanMiddlewareBackupToCluster>().eq("backup_address_id", backupAddress.getId()));
        backupToClusters.forEach(backupToCluster -> {
            clusterIds.add(backupToCluster.getClusterId());
        });
        return backupDTO.setClusterIds(clusterIds);
    }

    @Override
    public void deleteBackupAddress(Integer id, String clusterId) {
        QueryWrapper<BeanMiddlewareBackupToCluster> wrapper = new QueryWrapper<>();
        if (StringUtils.isEmpty(clusterId)) {
            wrapper.eq("backup_address_id", id);
//            deleteBucket(accessKeyId, secretAccessKey, bucketName, endpoint);
            middlewareBackupAddressMapper.delete(new QueryWrapper<BeanMiddlewareBackupAddress>().eq("id", id));
            backupAddressClusterMapper.delete(wrapper);
        } else {
            wrapper.eq("backup_address_id", id).eq("cluster_id", clusterId);
            backupAddressClusterMapper.delete(wrapper);
        }
    }

    @Override
    public void checkMinio(MiddlewareClusterBackupAddressDTO middlewareClusterBackupAddressDTO) {
        Minio minio = new Minio();
        BeanUtils.copyProperties(middlewareClusterBackupAddressDTO, minio);
        minioWrapper.checkMinio(minio);
    }

    /**
     * 删除bucket
     */
    private void deleteBucket(String accessKeyId, String secretAccessKey, String bucketName, String endpoint) {
        Minio minio = new Minio().setAccessKeyId(accessKeyId).setSecretAccessKey(secretAccessKey)
            .setBucketName(bucketName).setEndpoint(endpoint);
        minioWrapper.deleteBucket(minio);
    }
}
