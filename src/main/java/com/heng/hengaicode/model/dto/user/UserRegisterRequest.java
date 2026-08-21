package com.heng.hengaicode.model.dto.user;


import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 用户注册请求DTO
 */
@Data
public class UserRegisterRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    // 账号
    private String userAccount;
    // 密码
    private String userPassword;
    // 确认密码
    private String confirmPassword;

}
