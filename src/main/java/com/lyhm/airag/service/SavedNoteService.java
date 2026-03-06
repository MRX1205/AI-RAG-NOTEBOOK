package com.lyhm.airag.service;

import com.lyhm.airag.model.dto.note.SaveNoteRequest;
import com.lyhm.airag.model.dto.note.UpdateNoteRequest;
import com.lyhm.airag.model.entity.SavedNote;
import com.lyhm.airag.model.vo.SavedNoteVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;

/**
 * 保存笔记服务接口
 */
public interface SavedNoteService extends IService<SavedNote> {

    /**
     * 保存一条 AI 对话笔记
     *
     * @param userId  当前登录用户ID
     * @param request 保存请求体
     * @return 新笔记ID
     */
    Long saveNote(Long userId, SaveNoteRequest request);

    /**
     * 分页获取当前用户的笔记列表（支持按笔记本过滤）
     *
     * @param userId     用户ID
     * @param pageNum    页码（从1开始）
     * @param pageSize   每页大小
     * @param notebookId 可选，按笔记本ID过滤
     * @return 分页结果
     */
    Page<SavedNoteVO> listMyNotes(Long userId, long pageNum, long pageSize, Long notebookId);

    /**
     * 修改笔记标题和内容（仅本人可操作）
     *
     * @param noteId  笔记ID
     * @param userId  当前用户ID（用于鉴权）
     * @param request 修改请求
     */
    void updateNote(Long noteId, Long userId, UpdateNoteRequest request);

    /**
     * 删除笔记（逻辑删除，仅本人可操作）
     *
     * @param noteId 笔记ID
     * @param userId 当前用户ID（用于鉴权）
     */
    void deleteNote(Long noteId, Long userId);
}
