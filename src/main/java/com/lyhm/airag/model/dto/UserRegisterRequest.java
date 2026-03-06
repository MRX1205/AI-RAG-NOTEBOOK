package com.lyhm.airag.model.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 3191241716373120793L;

    /**
     * 账号
     */
    private String userAccount;

    /**
     * 密码
     */
    private String userPassword;

    /**
     * 确认密码
     */
    private String checkPassword;

    /**
     * 注册角色：user（普通用户）/ admin（管理员），默认 user
     */
    private String role;

    /**
     * 管理员注册码（role=admin 时必填）
     */
    private String adminCode;
}
