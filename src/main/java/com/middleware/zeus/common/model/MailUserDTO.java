package com.middleware.zeus.common.model;

import com.middleware.zeus.common.model.user.UserDto;
import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * @author yushuaikang
 * @date 2021/11/24 下午4:08
 */
@Accessors(chain = true)
@Data
public class MailUserDTO {

    private List<UserDto> users;

    private List<UserDto> userBy;

}
