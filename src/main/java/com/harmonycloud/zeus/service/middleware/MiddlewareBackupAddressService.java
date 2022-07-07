package com.harmonycloud.zeus.service.middleware;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.harmonycloud.caas.common.model.middleware.MiddlewareClusterBackupAddressDTO;
import com.harmonycloud.zeus.bean.BeanMiddlewareBackupAddress;

import java.util.List;

/**
 * @author yushuaikang
 * @date 2022/6/9 14:50 下午
 */
public interface MiddlewareBackupAddressService {

    /**
     * 创建备份位置
     *
     * @param middlewareClusterBackupAddressDTO 备份位置信息
     */
    void createBackupAddress(MiddlewareClusterBackupAddressDTO middlewareClusterBackupAddressDTO);

    /**
     * 更改备份位置
     *
     * @param middlewareClusterBackupAddressDTO 备份位置信息
     */
    void updateBackupAddress(MiddlewareClusterBackupAddressDTO middlewareClusterBackupAddressDTO);

    /**
     * 查询备份位置
     *
     * @param addressId 标识
     * @param keyWord   关键词
     * @return
     */
    List<MiddlewareClusterBackupAddressDTO> listBackupAddress(String addressId, String keyWord);

    /**
     * 查询备份位置详情
     *
     * @param id
     * @return
     */
    MiddlewareClusterBackupAddressDTO detail(Integer id);

    /**
     * 删除备份位置
     *
     * @param id        id
     * @param clusterId 集群ID
     */
    void deleteBackupAddress(Integer id, String clusterId);

    /**
     * minio连接校验
     *
     * @param middlewareClusterBackupAddressDTO 备份位置信息
     */
    void checkMinio(MiddlewareClusterBackupAddressDTO middlewareClusterBackupAddressDTO);

}
