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
import com.lyhm.airag.model.dto.notebook.NotebookAddRequest;
import com.lyhm.airag.model.dto.notebook.NotebookAdminUpdateRequest;
import com.lyhm.airag.model.dto.notebook.NotebookFeaturedRequest;
import com.lyhm.airag.model.dto.notebook.NotebookUpdateRequest;
import com.lyhm.airag.model.entity.Notebook;
import com.lyhm.airag.model.entity.User;
import com.lyhm.airag.model.vo.FeaturedNotebookVO;
import com.lyhm.airag.model.vo.NotebookVO;
import com.lyhm.airag.service.NotebookService;
import com.lyhm.airag.service.UserService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 笔记本控制器
 */
@RestController
@RequestMapping("/notebook")
public class NotebookController {

    @Resource
    private NotebookService notebookService;

    @Resource
    private UserService userService;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    // ==================== 公开接口（无需登录）====================

    /**
     * 获取精选笔记本列表（公开，最多12条）
     */
    @GetMapping("/featured")
    public BaseResponse<List<FeaturedNotebookVO>> getFeaturedNotebooks() {
        List<FeaturedNotebookVO> list = notebookService.getFeaturedNotebooks();
        return ResultUtils.success(list);
    }

    // ==================== 普通用户接口（需登录）====================

    /**
     * 创建笔记本
     */
    @PostMapping("/add")
    public BaseResponse<Long> addNotebook(@RequestBody NotebookAddRequest notebookAddRequest,
            HttpServletRequest request) {
        ThrowUtils.throwIf(notebookAddRequest == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        Notebook notebook = new Notebook();
        BeanUtil.copyProperties(notebookAddRequest, notebook);
        notebook.setUserId(loginUser.getId());
        notebook.setSourceCount(0);
        ThrowUtils.throwIf(notebook.getTitle() == null || notebook.getTitle().isEmpty(),
                ErrorCode.PARAMS_ERROR, "笔记本标题不能为空");
        boolean result = notebookService.save(notebook);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(notebook.getId());
    }

    /**
     * 获取当前用户的笔记本列表
     */
    @GetMapping("/list")
    public BaseResponse<List<NotebookVO>> listNotebook(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", loginUser.getId())
                .orderBy("createTime", false);
        List<Notebook> notebookList = notebookService.list(queryWrapper);
        return ResultUtils.success(notebookService.getNotebookVOList(notebookList));
    }

    /**
     * 获取笔记本详情
     */
    @GetMapping("/get")
    public BaseResponse<NotebookVO> getNotebookById(@RequestParam Long id,
            HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Notebook notebook = notebookService.getById(id);
        ThrowUtils.throwIf(notebook == null, ErrorCode.NOT_FOUND_ERROR, "笔记本不存在");
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!notebook.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权访问该笔记本");
        return ResultUtils.success(notebookService.getNotebookVO(notebook));
    }

    /**
     * 更新笔记本（用户自己，支持coverImage等字段）
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateNotebook(@RequestBody NotebookUpdateRequest notebookUpdateRequest,
            HttpServletRequest request) {
        if (notebookUpdateRequest == null || notebookUpdateRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Notebook oldNotebook = notebookService.getById(notebookUpdateRequest.getId());
        ThrowUtils.throwIf(oldNotebook == null, ErrorCode.NOT_FOUND_ERROR, "笔记本不存在");
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!oldNotebook.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权修改该笔记本");
        Notebook notebook = new Notebook();
        BeanUtil.copyProperties(notebookUpdateRequest, notebook);
        boolean result = notebookService.updateById(notebook);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 删除笔记本
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteNotebook(@RequestBody DeleteRequest deleteRequest,
            HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Notebook notebook = notebookService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(notebook == null, ErrorCode.NOT_FOUND_ERROR, "笔记本不存在");
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!notebook.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权删除该笔记本");
        notebookService.deleteNotebookCascade(deleteRequest.getId());
        return ResultUtils.success(true);
    }

    /**
     * 上传笔记本封面（需登录 + 归属校验）
     */
    @PostMapping("/upload/cover/{notebookId}")
    public BaseResponse<String> uploadCover(@PathVariable Long notebookId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        ThrowUtils.throwIf(notebookId == null || notebookId <= 0, ErrorCode.PARAMS_ERROR);
        Notebook notebook = notebookService.getById(notebookId);
        ThrowUtils.throwIf(notebook == null, ErrorCode.NOT_FOUND_ERROR, "笔记本不存在");
        User loginUser = userService.getLoginUser(request);
        ThrowUtils.throwIf(!notebook.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权操作该笔记本");
        String coverUrl = notebookService.uploadCover(notebookId, file, uploadDir);
        return ResultUtils.success(coverUrl);
    }

    // ==================== 管理员接口 ====================

    /**
     * 管理员分页获取所有笔记本
     */
    @GetMapping("/admin/list")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<NotebookVO>> adminListNotebooks(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String userName,
            HttpServletRequest request) {
        Page<NotebookVO> page = notebookService.getAdminNotebookPage(pageNum, pageSize, title, userName);
        return ResultUtils.success(page);
    }

    /**
     * 管理员修改任意笔记本（title, description, isFeatured）
     */
    @PutMapping("/admin/update/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminUpdateNotebook(@PathVariable Long id,
            @RequestBody NotebookAdminUpdateRequest req,
            HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0 || req == null, ErrorCode.PARAMS_ERROR);
        Notebook old = notebookService.getById(id);
        ThrowUtils.throwIf(old == null, ErrorCode.NOT_FOUND_ERROR, "笔记本不存在");
        Notebook notebook = new Notebook();
        notebook.setId(id);
        if (req.getTitle() != null)
            notebook.setTitle(req.getTitle());
        if (req.getDescription() != null)
            notebook.setDescription(req.getDescription());
        if (req.getSummary() != null)
            notebook.setSummary(req.getSummary());
        if (req.getIsFeatured() != null)
            notebook.setIsFeatured(req.getIsFeatured());
        boolean result = notebookService.updateById(notebook);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员快捷切换精选状态
     */
    @PutMapping("/admin/featured/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminToggleFeatured(@PathVariable Long id,
            @RequestBody NotebookFeaturedRequest req,
            HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0 || req == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(req.getIsFeatured() == null, ErrorCode.PARAMS_ERROR, "isFeatured不能为空");
        Notebook notebook = new Notebook();
        notebook.setId(id);
        notebook.setIsFeatured(req.getIsFeatured());
        boolean result = notebookService.updateById(notebook);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 管理员软删除笔记本（仅设 isDelete=1，不级联删除）
     * 接口：DELETE /notebook/admin/delete/{id}
     * 权限：ADMIN
     */
    @DeleteMapping("/admin/delete/{id}")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> adminDeleteNotebook(@PathVariable Long id,
            HttpServletRequest request) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        Notebook notebook = notebookService.getById(id);
        ThrowUtils.throwIf(notebook == null, ErrorCode.NOT_FOUND_ERROR, "笔记本不存在");
        boolean result = notebookService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
}
