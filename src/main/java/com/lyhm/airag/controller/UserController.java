package com.lyhm.airag.controller;

import cn.hutool.core.bean.BeanUtil;
import com.lyhm.airag.annotation.AuthCheck;
import com.lyhm.airag.common.BaseResponse;
import com.lyhm.airag.common.DeleteRequest;
import com.lyhm.airag.common.ResultUtils;
import com.lyhm.airag.constant.UserConstant;
import com.lyhm.airag.exception.BusinessException;
import com.lyhm.airag.exception.ErrorCode;
import com.lyhm.airag.exception.ThrowUtils;
import com.lyhm.airag.model.dto.UserLoginRequest;
import com.lyhm.airag.model.dto.UserRegisterRequest;
import com.lyhm.airag.model.dto.user.UserAddRequest;
import com.lyhm.airag.model.dto.user.UserAdminUpdateRequest;
import com.lyhm.airag.model.dto.user.UserQueryRequest;
import com.lyhm.airag.model.dto.user.UserStatusRequest;
import com.lyhm.airag.model.dto.user.UserUpdatePasswordRequest;
import com.lyhm.airag.model.dto.user.UserUpdateRequest;
import com.lyhm.airag.model.dto.user.UserUpdateUsernameRequest;
import com.lyhm.airag.model.entity.User;
import com.lyhm.airag.model.vo.LoginUserVO;
import com.lyhm.airag.model.vo.UserVO;
import com.lyhm.airag.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    // ==================== 公开接口 ====================

    /**
     * 功能描述：用户注册（支持角色选择，管理员需填注册码）
     * 接口路径：POST /user/register
     * 权限要求：PUBLIC
     */
    @PostMapping("register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        String role = userRegisterRequest.getRole();
        String adminCode = userRegisterRequest.getAdminCode();
        long result = userService.userRegister(userAccount, userPassword, checkPassword, role, adminCode);
        return ResultUtils.success(result);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        LoginUserVO loginUserVO = userService.userLogin(userAccount, userPassword, request);
        return ResultUtils.success(loginUserVO);
    }

    /**
     * 获取登录用户信息
     */
    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    /**
     * 用户注销
     */
    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }

    // ==================== 普通用户接口（需登录）====================

    /**
     * 修改自己的用户名
     */
    @PutMapping("/update/username")
    public BaseResponse<Boolean> updateUsername(@RequestBody UserUpdateUsernameRequest req,
            HttpServletRequest request) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        userService.updateUsername(loginUser.getId(), req.getNewUserName());
        return ResultUtils.success(true);
    }

    /**
     * 修改自己的密码
     */
    @PutMapping("/update/password")
    public BaseResponse<Boolean> updatePassword(@RequestBody UserUpdatePasswordRequest req,
            HttpServletRequest request) {
        ThrowUtils.throwIf(req == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        userService.updatePassword(loginUser.getId(), req.getOldPassword(), req.getNewPassword());
        return ResultUtils.success(true);
    }

    /**
     * 上传头像
     */
    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadAvatar(@RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        String avatarUrl = userService.uploadAvatar(loginUser.getId(), file, uploadDir);
        return ResultUtils.success(avatarUrl);
    }

    // ==================== 管理员接口（需 admin 角色）====================

    /**
     * 创建用户（管理员）
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtil.copyProperties(userAddRequest, user);
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 分页获取用户列表（管理员）—— GET 版本，支持 userName 模糊搜索
     */
    @GetMapping("/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUser(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String userName,
            HttpServletRequest request) {
        UserQueryRequest queryRequest = new UserQueryRequest();
        queryRequest.setPageNum(pageNum);
        queryRequest.setPageSize(pageSize);
        queryRequest.setUserName(userName);
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(queryRequest));
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 分页获取用户封装列表（管理员）—— POST Body 版本，保留旧接口兼容性
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        long pageNum = userQueryRequest.getPageNum();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(Page.of(pageNum, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(pageNum, pageSize, userPage.getTotalRow());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

    /**
     * 管理员修改指定用户信息（userName, userRole）
     */
    @PutMapping("/update/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminUpdateUser(@PathVariable Long id,
            @RequestBody UserAdminUpdateRequest req) {
        ThrowUtils.throwIf(req == null || id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User user = new User();
        user.setId(id);
        if (req.getUserName() != null)
            user.setUserName(req.getUserName());
        if (req.getUserRole() != null)
            user.setUserRole(req.getUserRole());
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户（管理员）
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest.getId());
        return ResultUtils.success(b);
    }

    /**
     * 更新用户（管理员，旧接口保留）
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updateUser(@RequestBody UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null || userUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User user = new User();
        BeanUtil.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员重置用户密码为默认值（12345678）
     * 接口：PUT /user/admin/reset-password/{id}
     * 权限：ADMIN
     */
    @PutMapping("/admin/reset-password/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<String> adminResetPassword(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        String defaultPwd = userService.adminResetPassword(id);
        return ResultUtils.success("密码已重置为：" + defaultPwd);
    }

    /**
     * 管理员修改用户账号状态（启用/禁用）
     * 接口：PUT /user/admin/status/{id}
     * 权限：ADMIN
     */
    @PutMapping("/admin/status/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminUpdateStatus(@PathVariable Long id,
            @RequestBody UserStatusRequest req) {
        ThrowUtils.throwIf(id == null || id <= 0 || req == null, ErrorCode.PARAMS_ERROR);
        userService.adminUpdateStatus(id, req.getStatus());
        return ResultUtils.success(true);
    }
}
