package com.lyhm.airag.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.lyhm.airag.exception.BusinessException;
import com.lyhm.airag.exception.ErrorCode;
import com.lyhm.airag.mapper.SavedNoteMapper;
import com.lyhm.airag.model.dto.note.SaveNoteRequest;
import com.lyhm.airag.model.dto.note.UpdateNoteRequest;
import com.lyhm.airag.model.entity.SavedNote;
import com.lyhm.airag.model.vo.SavedNoteVO;
import com.lyhm.airag.service.SavedNoteService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 保存笔记服务实现
 */
@Service
public class SavedNoteServiceImpl extends ServiceImpl<SavedNoteMapper, SavedNote> implements SavedNoteService {

    @Override
    public Long saveNote(Long userId, SaveNoteRequest request) {
        if (StrUtil.isBlank(request.getContent())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "笔记内容不能为空");
        }
        SavedNote note = new SavedNote();
        note.setUserId(userId);
        note.setNotebookId(request.getNotebookId());
        // 标题为空时使用问题前20字作为标题
        if (StrUtil.isBlank(request.getTitle())) {
            String question = request.getSourceQuestion();
            if (StrUtil.isNotBlank(question)) {
                note.setTitle(question.length() > 20 ? question.substring(0, 20) : question);
            } else {
                note.setTitle("AI笔记");
            }
        } else {
            note.setTitle(request.getTitle());
        }
        note.setContent(request.getContent());
        note.setSourceQuestion(request.getSourceQuestion());
        boolean result = this.save(note);
        if (!result) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "保存笔记失败");
        }
        return note.getId();
    }

    @Override
    public Page<SavedNoteVO> listMyNotes(Long userId, long pageNum, long pageSize, Long notebookId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("userId", userId)
                .eq("notebookId", notebookId, notebookId != null)
                .orderBy("createTime", false);
        Page<SavedNote> page = this.page(new Page<>(pageNum, pageSize), queryWrapper);
        // 转换为 VO
        List<SavedNoteVO> voList = page.getRecords().stream().map(note -> {
            SavedNoteVO vo = new SavedNoteVO();
            BeanUtil.copyProperties(note, vo);
            return vo;
        }).collect(Collectors.toList());
        Page<SavedNoteVO> voPage = new Page<>(pageNum, pageSize);
        voPage.setRecords(voList);
        voPage.setTotalRow(page.getTotalRow());
        return voPage;
    }

    @Override
    public void updateNote(Long noteId, Long userId, UpdateNoteRequest request) {
        SavedNote dbNote = this.getById(noteId);
        if (dbNote == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "笔记不存在");
        }
        if (!dbNote.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限修改他人笔记");
        }
        SavedNote note = new SavedNote();
        note.setId(noteId);
        if (StrUtil.isNotBlank(request.getTitle())) {
            note.setTitle(request.getTitle());
        }
        if (StrUtil.isNotBlank(request.getContent())) {
            note.setContent(request.getContent());
        }
        boolean result = this.updateById(note);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "修改笔记失败");
        }
    }

    @Override
    public void deleteNote(Long noteId, Long userId) {
        SavedNote dbNote = this.getById(noteId);
        if (dbNote == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "笔记不存在");
        }
        if (!dbNote.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限删除他人笔记");
        }
        boolean result = this.removeById(noteId);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "删除笔记失败");
        }
    }
}
