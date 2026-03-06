package com.lyhm.airag.mapper;

import com.lyhm.airag.model.entity.SavedNote;
import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 保存笔记 Mapper 接口
 */
@Mapper
public interface SavedNoteMapper extends BaseMapper<SavedNote> {
}
