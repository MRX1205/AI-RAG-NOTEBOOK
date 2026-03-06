package com.lyhm.airag.service;

import com.lyhm.airag.model.entity.Source;
import com.lyhm.airag.model.vo.SourceVO;
import com.mybatisflex.core.service.IService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 来源服务接口
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
public interface SourceService extends IService<Source> {

    /**
     * 上传文件来源
     *
     * @param file       上传的文件
     * @param notebookId 所属笔记本 ID
     * @param userId     上传用户 ID
     * @return 来源 VO
     */
    SourceVO uploadFileSource(MultipartFile file, Long notebookId, Long userId);

    /**
     * 添加文本来源
     *
     * @param title      来源标题
     * @param content    文本内容
     * @param notebookId 所属笔记本 ID
     * @param userId     用户 ID
     * @return 来源 VO
     */
    SourceVO addTextSource(String title, String content, Long notebookId, Long userId);

    /**
     * 删除来源（同时清理向量存储中的数据）
     *
     * @param sourceId   来源 ID
     * @param notebookId 所属笔记本 ID
     */
    void deleteSource(Long sourceId, Long notebookId);

    /**
     * Entity → VO 转换
     */
    SourceVO getSourceVO(Source source);

    /**
     * 批量 Entity → VO 转换
     */
    List<SourceVO> getSourceVOList(List<Source> sourceList);
}
