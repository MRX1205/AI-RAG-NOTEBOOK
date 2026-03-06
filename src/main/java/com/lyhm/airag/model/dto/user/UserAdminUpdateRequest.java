package com.lyhm.airag.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 管理员修改用户请求
 */
@Data
public class UserAdminUpdateRequest implements Serializable {

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户角色：user / admin
     */
    private String userRole;
}
