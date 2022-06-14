package com.harmonycloud.zeus.service.middleware;

import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterBackupAddressDTO;

import java.util.List;

/**
 * @author yushuaikang
 * @date 2022/6/9 14:50 下午
 */
public interface MiddlewareBackupAddressService {

    /**
     * 创建备份位置
     *
     * @param clusters                          集群ID集合
     * @param middlewareClusterBackupAddressDTO 备份位置信息
     */
    void createBackupAddress(List<String> clusters, MiddlewareClusterBackupAddressDTO middlewareClusterBackupAddressDTO);

    /**
     * 更改备份位置
     *
     * @param middlewareClusterBackupAddressDTO 备份位置信息
     */
    void updateBackupAddress(MiddlewareClusterBackupAddressDTO middlewareClusterBackupAddressDTO);

    /**
     * 查询备份位置
     *
     * @param keyWord 关键词
     * @return
     */
    List<MiddlewareClusterBackupAddressDTO> listBackupAddress(String keyWord);

    /**
     * 删除备份位置
     *
     * @param accessKeyId       用户名
     * @param secretAccessKey   密码
     * @param bucketName        bucket名称
     * @param endpoint          地址
     * @param name              中文名称
     * @param id                ID
     */
    void deleteBackupAddress(String accessKeyId, String secretAccessKey, String bucketName, String endpoint,
                             String name, Integer id);

    /**
     * minio连接校验
     *
     * @param middlewareClusterBackupAddressDTO 备份位置信息
     */
    void checkMinio(MiddlewareClusterBackupAddressDTO middlewareClusterBackupAddressDTO);

}
