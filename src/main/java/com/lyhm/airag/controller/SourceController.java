package com.lyhm.airag.controller;

import com.lyhm.airag.common.BaseResponse;
import com.lyhm.airag.common.DeleteRequest;
import com.lyhm.airag.common.ResultUtils;
import com.lyhm.airag.exception.BusinessException;
import com.lyhm.airag.exception.ErrorCode;
import com.lyhm.airag.exception.ThrowUtils;
import com.lyhm.airag.model.dto.source.SourceTextAddRequest;
import com.lyhm.airag.model.entity.Notebook;
import com.lyhm.airag.model.entity.Source;
import com.lyhm.airag.model.entity.User;
import com.lyhm.airag.model.vo.SourceVO;
import com.lyhm.airag.service.NotebookService;
import com.lyhm.airag.service.SourceService;
import com.lyhm.airag.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 来源控制器
 * <p>
 * 提供文件上传、文本来源、来源列表和删除接口。
 * 上传的文件会经过解析、分块、向量化后存入向量数据库。
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@RestController
@RequestMapping("/source")
public class SourceController {

    @Resource
    private SourceService sourceService;

    @Resource
    private NotebookService notebookService;

    @Resource
    private UserService userService;

    /**
     * 上传文件来源
     * <p>
     * 接收文件和笔记本 ID，执行完整的 RAG 处理链路：
     * 文件存储 → 文档解析 → 文本分块 → 向量化 → 存入向量存储
     * </p>
     *
     * @param file       上传的文件（PDF/TXT/MD/DOCX）
     * @param notebookId 所属笔记本 ID
     * @param request    HTTP 请求
     * @return 来源 VO
     */
    @PostMapping("/upload")
    public BaseResponse<SourceVO> uploadSource(
            @RequestParam("file") MultipartFile file,
            @RequestParam("notebookId") Long notebookId,
            HttpServletRequest request) {
        // 1. 参数校验
        ThrowUtils.throwIf(file == null || file.isEmpty(), ErrorCode.PARAMS_ERROR, "请选择要上传的文件");
        ThrowUtils.throwIf(notebookId == null || notebookId <= 0, ErrorCode.PARAMS_ERROR);

        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 3. 校验笔记本归属权
        checkNotebookOwnership(notebookId, loginUser.getId());

        // 4. 执行上传和处理
        SourceVO sourceVO = sourceService.uploadFileSource(file, notebookId, loginUser.getId());
        return ResultUtils.success(sourceVO);
    }

    /**
     * 添加文本来源
     * <p>
     * 用户手动输入或粘贴文本作为来源
     * </p>
     */
    @PostMapping("/add/text")
    public BaseResponse<SourceVO> addTextSource(
            @RequestBody SourceTextAddRequest sourceTextAddRequest,
            HttpServletRequest request) {
        // 1. 参数校验
        ThrowUtils.throwIf(sourceTextAddRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(sourceTextAddRequest.getNotebookId() == null, ErrorCode.PARAMS_ERROR, "笔记本 ID 不能为空");
        ThrowUtils.throwIf(sourceTextAddRequest.getTitle() == null || sourceTextAddRequest.getTitle().isEmpty(),
                ErrorCode.PARAMS_ERROR, "标题不能为空");
        ThrowUtils.throwIf(sourceTextAddRequest.getContent() == null || sourceTextAddRequest.getContent().isEmpty(),
                ErrorCode.PARAMS_ERROR, "内容不能为空");

        // 2. 获取当前登录用户
        User loginUser = userService.getLoginUser(request);

        // 3. 校验笔记本归属权
        checkNotebookOwnership(sourceTextAddRequest.getNotebookId(), loginUser.getId());

        // 4. 添加文本来源
        SourceVO sourceVO = sourceService.addTextSource(
                sourceTextAddRequest.getTitle(),
                sourceTextAddRequest.getContent(),
                sourceTextAddRequest.getNotebookId(),
                loginUser.getId());
        return ResultUtils.success(sourceVO);
    }

    /**
     * 获取来源列表
     *
     * @param notebookId 笔记本 ID
     * @param request    HTTP 请求
     * @return 来源 VO 列表
     */
    @GetMapping("/list")
    public BaseResponse<List<SourceVO>> listSources(
            @RequestParam Long notebookId,
            HttpServletRequest request) {
        ThrowUtils.throwIf(notebookId == null || notebookId <= 0, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);
        checkNotebookOwnership(notebookId, loginUser.getId());

        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("notebookId", notebookId)
                .orderBy("createTime", false);
        List<Source> sourceList = sourceService.list(queryWrapper);

        return ResultUtils.success(sourceService.getSourceVOList(sourceList));
    }

    /**
     * 删除来源
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSource(
            @RequestBody DeleteRequest deleteRequest,
            HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        User loginUser = userService.getLoginUser(request);

        // 查询来源确认归属
        Source source = sourceService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(source == null, ErrorCode.NOT_FOUND_ERROR, "来源不存在");
        ThrowUtils.throwIf(!source.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权删除该来源");

        // 删除来源（包括向量存储清理）
        sourceService.deleteSource(deleteRequest.getId(), source.getNotebookId());

        return ResultUtils.success(true);
    }

    /**
     * 校验笔记本归属权
     */
    private void checkNotebookOwnership(Long notebookId, Long userId) {
        Notebook notebook = notebookService.getById(notebookId);
        ThrowUtils.throwIf(notebook == null, ErrorCode.NOT_FOUND_ERROR, "笔记本不存在");
        ThrowUtils.throwIf(!notebook.getUserId().equals(userId),
                ErrorCode.NO_AUTH_ERROR, "无权访问该笔记本");
    }
}
