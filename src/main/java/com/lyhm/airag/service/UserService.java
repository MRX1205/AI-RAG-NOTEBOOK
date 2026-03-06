package com.lyhm.airag.service;

import com.lyhm.airag.model.dto.user.UserQueryRequest;
import com.lyhm.airag.model.vo.LoginUserVO;
import com.lyhm.airag.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;
import com.lyhm.airag.model.entity.User;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * 服务层。
 *
 * @author <a href="https:lyhlz.cn">掠影航猫</a>
 */
public interface UserService extends IService<User> {

    /**
     * 用户注册（基础版，角色默认为 user）
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

    /**
     * 用户注册（支持角色选择）
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param role          注册角色（user/admin）
     * @param adminCode     管理员注册码（role=admin 时校验）
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String role, String adminCode);

    /**
     * 加密后的密码
     * 
     * @param userPassword
     * @return
     */
    String getEncryptPassword(String userPassword);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏后单个用户的信息
     * * @param user
     * 
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏后用户列表的方法
     * 
     * @param userList
     * @return
     */
    List<UserVO> getUserVOList(List<User> userList);

    /**
     * 将查询请求转为QueryWrapper对象
     * 
     * @param userQueryRequest
     * @return
     */
    QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 修改用户名（当前登录用户）
     *
     * @param userId      用户ID
     * @param newUserName 新用户名（非空，长度≤20）
     */
    void updateUsername(Long userId, String newUserName);

    /**
     * 修改密码（当前登录用户）
     *
     * @param userId      用户ID
     * @param oldPassword 旧密码（原始明文）
     * @param newPassword 新密码（原始明文，长度6-20）
     */
    void updatePassword(Long userId, String oldPassword, String newPassword);

    /**
     * 上传头像并更新 user.userAvatar
     *
     * @param userId    用户ID
     * @param file      上传的图片文件
     * @param uploadDir 文件保存根目录（来源于配置）
     * @return 头像相对路径 URL
     */
    String uploadAvatar(Long userId, org.springframework.web.multipart.MultipartFile file, String uploadDir);

    /**
     * 管理员重置用户密码为默认值
     *
     * @param userId 目标用户ID
     * @return 重置后的明文密码（用于前端展示给管理员）
     */
    String adminResetPassword(Long userId);

    /**
     * 管理员修改用户账号状态（启用/禁用）
     *
     * @param userId 目标用户ID
     * @param status 0-禁用，1-启用
     */
    void adminUpdateStatus(Long userId, Integer status);

}
