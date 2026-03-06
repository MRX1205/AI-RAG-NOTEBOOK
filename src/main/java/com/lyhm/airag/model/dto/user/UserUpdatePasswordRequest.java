package com.lyhm.airag.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改密码请求
 */
@Data
public class UserUpdatePasswordRequest implements Serializable {

    /**
     * 旧密码
     */
    private String oldPassword;

    /**
     * 新密码（长度 6-20）
     */
    private String newPassword;
}
