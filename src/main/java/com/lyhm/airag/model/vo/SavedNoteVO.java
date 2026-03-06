package com.lyhm.airag.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 保存笔记视图对象
 */
@Data
public class SavedNoteVO implements Serializable {

    private Long id;

    private Long userId;

    private Long notebookId;

    private String title;

    private String content;

    private String sourceQuestion;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
