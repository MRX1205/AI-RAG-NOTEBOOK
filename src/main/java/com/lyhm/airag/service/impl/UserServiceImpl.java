package com.lyhm.airag.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.lyhm.airag.exception.BusinessException;
import com.lyhm.airag.exception.ErrorCode;
import com.lyhm.airag.model.dto.user.UserQueryRequest;
import com.lyhm.airag.model.enums.UserRoleEnum;
import com.lyhm.airag.model.vo.LoginUserVO;
import com.lyhm.airag.model.vo.UserVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.lyhm.airag.model.entity.User;
import com.lyhm.airag.mapper.UserMapper;
import com.lyhm.airag.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.lyhm.airag.constant.UserConstant.USER_LOGIN_STATE;

/**
 * 服务层实现。
 *
 * @author <a href="https:lyhlz.cn">掠影航猫</a>
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Value("${app.admin-register-code:1205}")
    private String adminRegisterCode;

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword) {
        return userRegister(userAccount, userPassword, checkPassword, UserRoleEnum.USER.getValue(), null);
    }

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String role, String adminCode) {
        // 1. 基础校验
        if (StrUtil.hasBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }
        // 2. 管理员注册码校验
        String finalRole = StrUtil.isBlank(role) ? UserRoleEnum.USER.getValue() : role;
        if (UserRoleEnum.ADMIN.getValue().equals(finalRole)) {
            if (StrUtil.isBlank(adminCode) || !adminRegisterCode.equals(adminCode)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "管理员注册码错误");
            }
        }
        // 3. 检查账号是否重复
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        long count = this.mapper.selectCountByQuery(queryWrapper);
        if (count > 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
        }
        // 4. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 5. 插入数据
        User user = new User();
        user.setUserAccount(userAccount);
        user.setUserPassword(encryptPassword);
        user.setUserName("无名");
        user.setUserRole(finalRole);
        boolean saveResult = this.save(user);
        if (!saveResult) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
        }
        return user.getId();
    }

    @Override
    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "lyhm";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtil.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, HttpServletRequest request) {
        // 1. 校验
        if (StrUtil.hasBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = getEncryptPassword(userPassword);
        // 查询用户是否存在
        QueryWrapper queryWrapper = new QueryWrapper();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.mapper.selectOneByQuery(queryWrapper);
        // 用户不存在
        if (user == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 检查账号状态
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "账号已被禁用，请联系管理员");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 4. 获得脱敏后的用户信息
        return this.getLoginUserVO(user);
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接返回上述结果）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtil.copyProperties(user, userVO);
        return userVO;
    }

    @Override
    public List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public QueryWrapper getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        return QueryWrapper.create()
                .eq("id", id)
                .eq("userRole", userRole)
                .like("userAccount", userAccount)
                .like("userName", userName)
                .like("userProfile", userProfile)
                .orderBy(sortField, "ascend".equals(sortOrder));
    }

    @Override
    public void updateUsername(Long userId, String newUserName) {
        if (StrUtil.isBlank(newUserName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名不能为空");
        }
        if (newUserName.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户名长度不能超过20个字符");
        }
        User user = new User();
        user.setId(userId);
        user.setUserName(newUserName);
        boolean result = this.updateById(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改用户名失败");
        }
    }

    @Override
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        if (StrUtil.hasBlank(oldPassword, newPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码不能为空");
        }
        if (newPassword.length() < 6 || newPassword.length() > 20) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "新密码长度必须为6-20位");
        }
        // 校验旧密码
        User dbUser = this.getById(userId);
        if (dbUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        String encryptOld = getEncryptPassword(oldPassword);
        if (!encryptOld.equals(dbUser.getUserPassword())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "旧密码不正确");
        }
        // 更新新密码
        String encryptNew = getEncryptPassword(newPassword);
        User user = new User();
        user.setId(userId);
        user.setUserPassword(encryptNew);
        boolean result = this.updateById(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改密码失败");
        }
    }

    @Override
    public String adminResetPassword(Long userId) {
        User dbUser = this.getById(userId);
        if (dbUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        final String DEFAULT_PASSWORD = "12345678";
        String encryptPassword = getEncryptPassword(DEFAULT_PASSWORD);
        User user = new User();
        user.setId(userId);
        user.setUserPassword(encryptPassword);
        boolean result = this.updateById(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "重置密码失败");
        }
        return DEFAULT_PASSWORD;
    }

    @Override
    public void adminUpdateStatus(Long userId, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "status 只能为 0 或 1");
        }
        User dbUser = this.getById(userId);
        if (dbUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在");
        }
        User user = new User();
        user.setId(userId);
        user.setStatus(status);
        boolean result = this.updateById(user);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改用户状态失败");
        }
    }

    @Override
    public String uploadAvatar(Long userId, org.springframework.web.multipart.MultipartFile file, String uploadDir) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }
        String extension = cn.hutool.core.io.FileUtil.extName(originalFilename).toLowerCase();
        if (!java.util.List.of("jpg", "jpeg", "png", "gif", "webp").contains(extension)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像只支持 jpg/png/gif/webp 格式");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "头像文件不能超过2MB");
        }
        // 保存文件（toAbsolutePath 避免 transferTo 把相对路径解析到 Tomcat 工作目录）
        String savedFileName = java.util.UUID.randomUUID() + "." + extension;
        java.nio.file.Path savePath = java.nio.file.Paths.get(uploadDir, "avatars", savedFileName)
                .toAbsolutePath().normalize();
        try {
            java.nio.file.Files.createDirectories(savePath.getParent());
            // 用 Files.copy + InputStream 代替 transferTo，规避 Tomcat 相对路径问题
            try (java.io.InputStream inputStream = file.getInputStream()) {
                java.nio.file.Files.copy(inputStream, savePath,
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (java.io.IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "头像保存失败");
        }
        // 相对路径（前端用于拼接完整URL）
        String avatarUrl = "/uploads/avatars/" + savedFileName;
        // 更新数据库
        User user = new User();
        user.setId(userId);
        user.setUserAvatar(avatarUrl);
        this.updateById(user);
        return avatarUrl;
    }

}
