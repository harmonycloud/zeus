package com.middleware.zeus.integration.minio;

import com.middleware.caas.common.enums.ErrorMessage;
import com.middleware.caas.common.exception.BusinessException;
import com.middleware.caas.common.exception.CaasRuntimeException;
import com.middleware.zeus.integration.cluster.bean.Minio;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * @author xutianhong
 * @Date 2021/4/6 11:42 上午
 */
@Component
@Slf4j
public class MinioWrapper {

    /**
     * 构建链接
     */
    public static MinioClient build(Minio minio) {
        return MinioClient.builder().endpoint(minio.getEndpoint()).credentials(minio.getAccessKeyId(), minio.getSecretAccessKey()).build();
    }

    /**
     * 获取object(备份)列表
     */
    public Iterable<Result<Item>> listObjects(Minio minio) throws Exception {
        MinioClient minioClient = MinioWrapper.build(minio);
        return minioClient.listObjects(ListObjectsArgs.builder().bucket(minio.getBucketName()).build());
    }

    /**
     * 删除object(备份)
     */
    public void removeObject(Minio minio, String objectName) throws Exception{
        if (StringUtils.isBlank(objectName)) {
            return;
        }
        try {
            MinioClient minioClient = MinioWrapper.build(minio);
            // 从mybucket中删除myobject。
            RemoveObjectArgs objectArgs = RemoveObjectArgs.builder().object(objectName)
                    .bucket(minio.getBucketName()).build();
            minioClient.removeObject(objectArgs);
        } catch (MinioException e) {
            throw new CaasRuntimeException(ErrorMessage.DELETE_BACKUP_FILE_FAILED);
        }
    }

    /**
     * 校验minio连接
     * @param minio
     */
    public void checkMinio(Minio minio) {
        try {
            MinioClient minioClient = MinioWrapper.build(minio);
            minioClient.listBuckets();
        } catch (Exception e) {
            log.error("连接失败:", e);
            throw new BusinessException(ErrorMessage.CONNECTION_FAILED);
        }
    }

    /**
     * 删除bucket
     */
    public void deleteBucket(Minio minio) {
        try {
            MinioClient minioClient = MinioWrapper.build(minio);
            // 删除之前先检查bucket是否存在。
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(minio.getBucketName()).build());
            if (found) {
                minioClient.removeBucket(RemoveBucketArgs.builder().bucket(minio.getBucketName()).build());
            }
        } catch (MinioException | IOException | InvalidKeyException | NoSuchAlgorithmException e) {
            log.error("bucket删除失败:", e);
        }
    }

}
