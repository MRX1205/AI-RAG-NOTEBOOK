package com.lyhm.airag.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 修改用户名请求
 */
@Data
public class UserUpdateUsernameRequest implements Serializable {

    /**
     * 新用户名（长度 1-20）
     */
    private String newUserName;
}
