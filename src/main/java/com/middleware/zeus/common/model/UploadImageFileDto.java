package com.middleware.zeus.common.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author yushuaikang
 * @date 2022/1/8 下午4:53
 */
@Accessors(chain = true)
@Data
public class UploadImageFileDto {

    private byte[] bytes;

    private String type;
}
