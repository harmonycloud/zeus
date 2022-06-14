package com.harmonycloud.zeus.service.middleware.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.harmonycloud.caas.common.constants.CommonConstant;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterBackupAddressDTO;
import com.harmonycloud.zeus.bean.BeanMiddlewareBackupAddress;
import com.harmonycloud.zeus.dao.BeanMiddlewareBackupAddressMapper;
import com.harmonycloud.zeus.integration.cluster.bean.Minio;
import com.harmonycloud.zeus.integration.minio.MinioWrapper;
import com.harmonycloud.zeus.service.middleware.MiddlewareBackupAddressService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
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

    @Override
    public void createBackupAddress(List<String> clusters,
        MiddlewareClusterBackupAddressDTO middlewareClusterBackupAddressDTO) {
        List<BeanMiddlewareBackupAddress> backupAddresses = new ArrayList<>();
        clusters.forEach(cluster -> {
            BeanMiddlewareBackupAddress backupAddress = new BeanMiddlewareBackupAddress();
            BeanUtils.copyProperties(middlewareClusterBackupAddressDTO, backupAddress);
            backupAddress.setClusterId(cluster);
            backupAddresses.add(backupAddress);
        });
        middlewareBackupAddressMapper.insertBatchSomeColumn(backupAddresses);
        try {
            checkMinio(middlewareClusterBackupAddressDTO);
            middlewareClusterBackupAddressDTO.setStatus(CommonConstant.NUM_ONE);
        } catch (Exception e) {
            middlewareClusterBackupAddressDTO.setStatus(CommonConstant.NUM_ZERO);
        }
        BeanMiddlewareBackupAddress backup = new BeanMiddlewareBackupAddress();
        BeanUtils.copyProperties(middlewareClusterBackupAddressDTO, backup);
        backup.setRelevanceNum(CommonConstant.NUM_ZERO);
        middlewareBackupAddressMapper.insert(backup);
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
    }

    @Override
    public List<MiddlewareClusterBackupAddressDTO> listBackupAddress(String keyWord) {
        QueryWrapper<BeanMiddlewareBackupAddress> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(keyWord)) {
            wrapper.eq("name", keyWord);
        }
        List<BeanMiddlewareBackupAddress> backups = middlewareBackupAddressMapper.selectList(wrapper);
        return backups.stream().map(backup -> {
            MiddlewareClusterBackupAddressDTO backupDTO = new MiddlewareClusterBackupAddressDTO();
            BeanUtils.copyProperties(backup, backupDTO);
            return backupDTO;
        }).collect(Collectors.toList());
    }

    @Override
    public void deleteBackupAddress(String accessKeyId, String secretAccessKey, String bucketName, String endpoint,
        String name, Integer id) {
        QueryWrapper<BeanMiddlewareBackupAddress> wrapper = new QueryWrapper<>();
        if (StringUtils.isNotEmpty(name)) {
            deleteBucket(accessKeyId, secretAccessKey, bucketName, endpoint);
            wrapper.eq("name", name);
        } else {
            wrapper.eq("id", id);
        }
        middlewareBackupAddressMapper.delete(wrapper);
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
