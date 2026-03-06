package com.lyhm.airag.model.dto.notebook;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * 更新笔记本请求 DTO
 * <p>
 * 前端调用更新笔记本接口时传递的参数。
 * 必须包含 id 字段，用于定位要更新的笔记本。
 * 仅传递需要修改的字段即可。
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Data
public class NotebookUpdateRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 笔记本ID（必填，用于定位要更新的记录）
     */
    private Long id;

    /**
     * 笔记本标题
     */
    private String title;

    /**
     * 笔记本描述
     */
    private String description;
}
