package com.lyhm.airag.controller;

import com.lyhm.airag.common.BaseResponse;
import com.lyhm.airag.common.ResultUtils;
import com.lyhm.airag.exception.ErrorCode;
import com.lyhm.airag.exception.ThrowUtils;
import com.lyhm.airag.model.vo.FeaturedNotebookVO;
import com.lyhm.airag.model.vo.PublicNotebookDetailVO;
import com.lyhm.airag.service.NotebookService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开笔记本接口（无需登录）
 * <p>
 * 提供精选笔记本的公开访问能力，未登录用户可浏览只读内容。
 * 对应前端 /explore 和 /explore/:id 页面。
 * </p>
 */
@RestController
@RequestMapping("/public/notebooks")
public class PublicNotebookController {

    @Resource
    private NotebookService notebookService;

    /**
     * 获取所有精选笔记本列表（公开，无需登录）
     * 接口路径：GET /public/notebooks/featured
     */
    @GetMapping("/featured")
    public BaseResponse<List<FeaturedNotebookVO>> getFeaturedNotebooks() {
        List<FeaturedNotebookVO> list = notebookService.getFeaturedNotebooks();
        return ResultUtils.success(list);
    }

    /**
     * 获取单个精选笔记本的公开详情（含来源列表，无需登录）
     * 接口路径：GET /public/notebooks/{id}
     * <p>
     * 只有 isFeatured=1 的笔记本才能通过此接口访问
     * </p>
     */
    @GetMapping("/{id}")
    public BaseResponse<PublicNotebookDetailVO> getPublicNotebookDetail(@PathVariable Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR);
        PublicNotebookDetailVO vo = notebookService.getPublicNotebookDetail(id);
        return ResultUtils.success(vo);
    }
}
