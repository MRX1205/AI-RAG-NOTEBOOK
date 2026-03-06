package com.lyhm.airag.service;

import com.lyhm.airag.model.entity.Notebook;
import com.lyhm.airag.model.vo.FeaturedNotebookVO;
import com.lyhm.airag.model.vo.NotebookVO;
import com.lyhm.airag.model.vo.PublicNotebookDetailVO;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 笔记本服务接口
 */
public interface NotebookService extends IService<Notebook> {

    NotebookVO getNotebookVO(Notebook notebook);

    List<NotebookVO> getNotebookVOList(List<Notebook> notebookList);

    void deleteNotebookCascade(Long notebookId);

    /**
     * 获取精选笔记本列表（公开，最多12条）
     */
    List<FeaturedNotebookVO> getFeaturedNotebooks();

    /**
     * 获取单个精选笔记本公开详情（含来源列表，无需登录）
     *
     * @param notebookId 笔记本 ID
     * @return 公开详情 VO，仅精选笔记本可访问
     */
    PublicNotebookDetailVO getPublicNotebookDetail(Long notebookId);

    /**
     * 克隆笔记本（为当前用户创建副本）
     * <p>
     * 复制笔记本元数据 + 所有来源记录 + 向量存储，原笔记本不受影响。
     * </p>
     *
     * @param notebookId 要克隆的精选笔记本 ID
     * @param userId     当前登录用户 ID
     * @return 新笔记本 ID
     */
    Long cloneNotebook(Long notebookId, Long userId);

    /**
     * 管理员分页查询所有笔记本，支持按标题、用户名过滤
     */
    Page<NotebookVO> getAdminNotebookPage(long pageNum, long pageSize, String title, String userName);

    /**
     * 上传笔记本封面，返回相对URL路径
     */
    String uploadCover(Long notebookId, MultipartFile file, String uploadDir);
}
