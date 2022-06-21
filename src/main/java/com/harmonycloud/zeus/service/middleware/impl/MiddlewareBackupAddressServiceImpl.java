package com.harmonycloud.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.constants.CommonConstant;
import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.BusinessException;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterBackupAddressDTO;
import com.harmonycloud.zeus.bean.BeanMiddlewareBackupAddress;
import com.harmonycloud.zeus.bean.BeanMiddlewareBackupToCluster;
import com.harmonycloud.zeus.config.MyLambdaUpdateWrapper;
import com.harmonycloud.zeus.dao.BeanBackupAddressClusterMapper;
import com.harmonycloud.zeus.dao.BeanMiddlewareBackupAddressMapper;
import com.harmonycloud.zeus.integration.cluster.bean.Minio;
import com.harmonycloud.zeus.integration.minio.MinioWrapper;
import com.harmonycloud.zeus.service.middleware.MiddlewareBackupAddressService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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

    @Override
    public void createBackupAddress(MiddlewareClusterBackupAddressDTO middlewareClusterBackupAddressDTO) {
        QueryWrapper<BeanMiddlewareBackupAddress> wrapper = new QueryWrapper<>();
        List<BeanMiddlewareBackupAddress> backupAddressList = middlewareBackupAddressMapper.selectList(wrapper);
        if (!CollectionUtils.isEmpty(backupAddressList)) {
            backupAddressList.forEach(beanMiddlewareBackupAddress -> {
                if (middlewareClusterBackupAddressDTO.getName().equals(beanMiddlewareBackupAddress.getName())) {
                    throw new BusinessException(ErrorMessage.CHINESE_NAME_REPETITION);
                }
            });
        }
        try {
            checkMinio(middlewareClusterBackupAddressDTO);
            middlewareClusterBackupAddressDTO.setStatus(CommonConstant.NUM_ONE);
        } catch (Exception e) {
            middlewareClusterBackupAddressDTO.setStatus(CommonConstant.NUM_ZERO);
        }
        BeanMiddlewareBackupAddress backup = new BeanMiddlewareBackupAddress();
        BeanUtils.copyProperties(middlewareClusterBackupAddressDTO, backup);
        backup.setRelevanceNum(CommonConstant.NUM_ZERO);
        backup.setCreateTime(new Date());
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
        try {
            checkMinio(middlewareClusterBackupAddressDTO);
            middlewareClusterBackupAddressDTO.setStatus(CommonConstant.NUM_ONE);
        } catch (Exception e) {
            middlewareClusterBackupAddressDTO.setStatus(CommonConstant.NUM_ZERO);
        }
        BeanMiddlewareBackupAddress backup = new BeanMiddlewareBackupAddress();
        BeanUtils.copyProperties(middlewareClusterBackupAddressDTO, backup);
        QueryWrapper<BeanMiddlewareBackupAddress> wrapper = new QueryWrapper<>();
        middlewareBackupAddressMapper.update(backup, wrapper);
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
    public List<MiddlewareClusterBackupAddressDTO> listBackupAddress(String name, String keyWord) {
        QueryWrapper<BeanMiddlewareBackupAddress> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(name)) {
            wrapper.eq("name", name);
        }
        List<BeanMiddlewareBackupAddress> backups = middlewareBackupAddressMapper.selectList(wrapper);
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

    @Override
    public void calRelevanceNum(String name, boolean flag) {
        MyLambdaUpdateWrapper<BeanMiddlewareBackupAddress> wrapper =
            new MyLambdaUpdateWrapper<>(BeanMiddlewareBackupAddress.class);
        if (flag) {
            wrapper.incrField(BeanMiddlewareBackupAddress::getRelevanceNum, 1);
        } else {
            wrapper.descField(BeanMiddlewareBackupAddress::getRelevanceNum, 1);
        }
        wrapper.eq(BeanMiddlewareBackupAddress::getName, name);
        middlewareBackupAddressMapper.update(null, wrapper);
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
