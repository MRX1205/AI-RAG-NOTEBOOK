package com.lyhm.airag.model.dto.user;

import lombok.Data;

/**
 * 管理员修改用户状态请求体
 */
@Data
public class UserStatusRequest {

    /**
     * 账号状态：0-禁用，1-启用
     */
    private Integer status;
}
