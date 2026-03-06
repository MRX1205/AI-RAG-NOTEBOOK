package com.lyhm.airag.service;

import com.lyhm.airag.model.entity.Notebook;
import com.lyhm.airag.model.vo.FeaturedNotebookVO;
import com.lyhm.airag.model.vo.NotebookVO;
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
     * 管理员分页查询所有笔记本，支持按标题、用户名过滤
     */
    Page<NotebookVO> getAdminNotebookPage(long pageNum, long pageSize, String title, String userName);

    /**
     * 上传笔记本封面，返回相对URL路径
     */
    String uploadCover(Long notebookId, MultipartFile file, String uploadDir);
}
