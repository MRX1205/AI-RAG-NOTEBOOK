package com.lyhm.airag.controller;

import com.lyhm.airag.common.BaseResponse;
import com.lyhm.airag.common.ResultUtils;
import com.lyhm.airag.exception.ErrorCode;
import com.lyhm.airag.exception.ThrowUtils;
import com.lyhm.airag.model.dto.note.SaveNoteRequest;
import com.lyhm.airag.model.dto.note.UpdateNoteRequest;
import com.lyhm.airag.model.entity.User;
import com.lyhm.airag.model.vo.SavedNoteVO;
import com.lyhm.airag.service.SavedNoteService;
import com.lyhm.airag.service.UserService;
import com.mybatisflex.core.paginate.Page;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

/**
 * 功能描述：AI对话笔记的保存、查询、修改、删除
 * 接口路径：/note/**
 * 权限要求：USER（需登录；修改/删除仅限本人）
 */
@RestController
@RequestMapping("/note")
public class SavedNoteController {

    @Resource
    private SavedNoteService savedNoteService;

    @Resource
    private UserService userService;

    /**
     * 功能描述：保存一条 AI 对话笔记
     * 接口路径：POST /note/save
     * 权限要求：USER
     */
    @PostMapping("/save")
    public BaseResponse<Long> saveNote(@RequestBody SaveNoteRequest request,
                                       HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        Long noteId = savedNoteService.saveNote(loginUser.getId(), request);
        return ResultUtils.success(noteId);
    }

    /**
     * 功能描述：分页获取当前用户笔记列表（支持按 notebookId 过滤）
     * 接口路径：GET /note/list
     * 权限要求：USER
     */
    @GetMapping("/list")
    public BaseResponse<Page<SavedNoteVO>> listMyNotes(
            @RequestParam(defaultValue = "1") long pageNum,
            @RequestParam(defaultValue = "10") long pageSize,
            @RequestParam(required = false) Long notebookId,
            HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        Page<SavedNoteVO> page = savedNoteService.listMyNotes(loginUser.getId(), pageNum, pageSize, notebookId);
        return ResultUtils.success(page);
    }

    /**
     * 功能描述：修改笔记标题和内容（仅本人）
     * 接口路径：PUT /note/{id}
     * 权限要求：USER
     */
    @PutMapping("/{id}")
    public BaseResponse<Boolean> updateNote(@PathVariable Long id,
                                            @RequestBody UpdateNoteRequest request,
                                            HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        savedNoteService.updateNote(id, loginUser.getId(), request);
        return ResultUtils.success(true);
    }

    /**
     * 功能描述：删除笔记（逻辑删除，仅本人）
     * 接口路径：DELETE /note/{id}
     * 权限要求：USER
     */
    @DeleteMapping("/{id}")
    public BaseResponse<Boolean> deleteNote(@PathVariable Long id,
                                            HttpServletRequest httpRequest) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(httpRequest);
        savedNoteService.deleteNote(id, loginUser.getId());
        return ResultUtils.success(true);
    }
}
