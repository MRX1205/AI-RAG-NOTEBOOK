package com.lyhm.airag.controller;

import com.lyhm.airag.annotation.AuthCheck;
import com.lyhm.airag.common.BaseResponse;
import com.lyhm.airag.common.ResultUtils;
import com.lyhm.airag.constant.UserConstant;
import com.lyhm.airag.model.vo.AdminStatsVO;
import com.lyhm.airag.service.NotebookService;
import com.lyhm.airag.service.SourceService;
import com.lyhm.airag.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员专属接口（除了 UserController/NotebookController 中已有的管理员接口）
 */
@RestController
@RequestMapping("/admin")
public class AdminController {

    @Resource
    private UserService userService;

    @Resource
    private NotebookService notebookService;

    @Resource
    private SourceService sourceService;

    /**
     * 管理员仪表盘统计数据
     * 接口路径：GET /admin/stats
     * 权限要求：ADMIN
     */
    @GetMapping("/stats")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<AdminStatsVO> getAdminStats(HttpServletRequest request) {
        long userCount = userService.count();
        long notebookCount = notebookService.count();
        long sourceCount = sourceService.count();
        return ResultUtils.success(new AdminStatsVO(userCount, notebookCount, sourceCount));
    }
}
