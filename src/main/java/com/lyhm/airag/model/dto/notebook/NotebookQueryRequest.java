package com.lyhm.airag.model.dto.notebook;

import com.lyhm.airag.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serial;

/**
 * 查询笔记本请求 DTO
 * <p>
 * 继承了 PageRequest，支持分页查询。
 * 可以按标题模糊搜索笔记本。
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class NotebookQueryRequest extends PageRequest {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 笔记本标题（支持模糊搜索）
     */
    private String title;
}
