package com.harmonycloud.zeus.integration.minio;

import com.harmonycloud.caas.common.enums.ErrorMessage;
import com.harmonycloud.caas.common.exception.CaasRuntimeException;
import com.harmonycloud.zeus.integration.cluster.bean.Minio;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * @author xutianhong
 * @Date 2021/4/6 11:42 上午
 */
@Component
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

}
