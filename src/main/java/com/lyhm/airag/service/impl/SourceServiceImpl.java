package com.lyhm.airag.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import com.lyhm.airag.config.VectorStoreConfig;
import com.lyhm.airag.exception.BusinessException;
import com.lyhm.airag.exception.ErrorCode;
import com.lyhm.airag.mapper.SourceMapper;
import com.lyhm.airag.model.entity.Notebook;
import com.lyhm.airag.model.entity.Source;
import com.lyhm.airag.model.vo.SourceVO;
import com.lyhm.airag.service.NotebookService;
import com.lyhm.airag.service.SourceService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.document.parser.apache.tika.ApacheTikaDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 来源服务实现类
 * <p>
 * 核心处理链路：文件上传 → 文档解析 → 文本分块 → 向量化 → 存入向量数据库
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Slf4j
@Service
public class SourceServiceImpl extends ServiceImpl<SourceMapper, Source>
        implements SourceService {

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private VectorStoreConfig vectorStoreConfig;

    @Resource
    private NotebookService notebookService;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /**
     * 支持的文件扩展名白名单
     */
    private static final List<String> ALLOWED_EXTENSIONS = List.of("pdf", "txt", "md", "docx");

    @Override
    public SourceVO uploadFileSource(MultipartFile file, Long notebookId, Long userId) {
        // 1. 验证文件
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isEmpty()) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
        }
        String extension = FileUtil.extName(originalFilename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "不支持的文件类型：" + extension + "，仅支持 " + ALLOWED_EXTENSIONS);
        }

        // 2. 保存文件到磁盘
        String savedFileName = UUID.randomUUID() + "." + extension;
        Path savePath = Paths.get(uploadDir, "sources", String.valueOf(notebookId), savedFileName);
        try {
            Files.createDirectories(savePath.getParent());
            file.transferTo(savePath.toFile());
        } catch (IOException e) {
            log.error("文件保存失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文件保存失败");
        }

        // 3. 创建来源记录（初始状态为 processing）
        Source source = Source.builder()
                .notebookId(notebookId)
                .userId(userId)
                .fileName(originalFilename)
                .fileType(extension)
                .filePath(savePath.toString())
                .fileSize(file.getSize())
                .status("processing")
                .build();
        this.save(source);

        // 4. 解析文档并向量化（同步处理）
        try {
            processDocument(source, savePath);
        } catch (Exception e) {
            log.error("文档处理失败: {}", originalFilename, e);
            source.setStatus("failed");
            source.setErrorMessage(e.getMessage());
            this.updateById(source);
            // 更新 notebook sourceCount
            updateNotebookSourceCount(notebookId);
            return getSourceVO(source);
        }

        // 5. 更新来源状态为 completed
        source.setStatus("completed");
        this.updateById(source);

        // 6. 更新笔记本的来源数量
        updateNotebookSourceCount(notebookId);

        // 7. 持久化向量存储
        vectorStoreConfig.persistStore(notebookId);

        return getSourceVO(source);
    }

    @Override
    public SourceVO addTextSource(String title, String content, Long notebookId, Long userId) {
        // 1. 创建来源记录
        Source source = Source.builder()
                .notebookId(notebookId)
                .userId(userId)
                .fileName(title)
                .fileType("text")
                .fileSize((long) content.getBytes(StandardCharsets.UTF_8).length)
                .content(content)
                .status("processing")
                .build();
        this.save(source);

        // 2. 直接对文本分块和向量化
        try {
            Document document = Document.from(content, Metadata.from("sourceId", source.getId().toString()));
            processDocumentSegments(source, document, notebookId);
        } catch (Exception e) {
            log.error("文本来源处理失败: {}", title, e);
            source.setStatus("failed");
            source.setErrorMessage(e.getMessage());
            this.updateById(source);
            updateNotebookSourceCount(notebookId);
            return getSourceVO(source);
        }

        source.setStatus("completed");
        this.updateById(source);
        updateNotebookSourceCount(notebookId);
        vectorStoreConfig.persistStore(notebookId);

        return getSourceVO(source);
    }

    @Override
    public void deleteSource(Long sourceId, Long notebookId) {
        // 1. 删除来源记录（逻辑删除）
        this.removeById(sourceId);

        // 2. 重建该笔记本的向量存储（简单方案：删除后重建）
        // 由于 InMemoryEmbeddingStore 不支持按 metadata 删除，
        // 这里采用重建方式：清除旧存储，重新加载剩余来源
        rebuildVectorStore(notebookId);

        // 3. 更新笔记本来源数量
        updateNotebookSourceCount(notebookId);
    }

    @Override
    public SourceVO getSourceVO(Source source) {
        if (source == null) {
            return null;
        }
        SourceVO sourceVO = new SourceVO();
        BeanUtil.copyProperties(source, sourceVO);
        return sourceVO;
    }

    @Override
    public List<SourceVO> getSourceVOList(List<Source> sourceList) {
        return sourceList.stream()
                .map(this::getSourceVO)
                .collect(Collectors.toList());
    }

    // ==================== 私有方法 ====================

    /**
     * 解析文件文档并向量化
     */
    private void processDocument(Source source, Path filePath) {
        // 使用 Apache Tika 解析文档
        ApacheTikaDocumentParser parser = new ApacheTikaDocumentParser();
        Document document;
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            document = parser.parse(inputStream);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "文档解析失败: " + e.getMessage());
        }

        // 添加元数据
        document.metadata().put("sourceId", source.getId().toString());
        document.metadata().put("fileName", source.getFileName());

        // 保存解析后的纯文本内容（限制长度用于预览）
        String textContent = document.text();
        if (textContent != null && textContent.length() > 10000) {
            source.setContent(textContent.substring(0, 10000) + "...");
        } else {
            source.setContent(textContent);
        }

        processDocumentSegments(source, document, source.getNotebookId());
    }

    /**
     * 对文档进行分块和向量化
     */
    private void processDocumentSegments(Source source, Document document, Long notebookId) {
        // 文本分块（递归分块，每块最多 500 字符，重叠 50 字符）
        DocumentSplitter splitter = DocumentSplitters.recursive(500, 50);
        List<TextSegment> segments = splitter.split(document);

        // 为每个分块添加 sourceId 元数据（用于后续按来源过滤检索）
        for (TextSegment segment : segments) {
            segment.metadata().put("sourceId", source.getId().toString());
        }

        log.info("文档 {} 分块完成，共 {} 个分块", source.getFileName(), segments.size());

        // 向量化
        List<Embedding> embeddings = embeddingModel.embedAll(segments).content();

        // 存入向量存储
        InMemoryEmbeddingStore<TextSegment> store = vectorStoreConfig.getOrCreateStore(notebookId);
        store.addAll(embeddings, segments);

        // 更新分块数量
        source.setSegmentCount(segments.size());
    }

    /**
     * 重建笔记本的向量存储
     */
    private void rebuildVectorStore(Long notebookId) {
        // 删除旧的向量存储
        vectorStoreConfig.removeStore(notebookId);

        // 查询该笔记本剩余的所有来源
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("notebookId", notebookId)
                .eq("status", "completed");
        List<Source> remainingSources = this.list(queryWrapper);

        if (remainingSources.isEmpty()) {
            return;
        }

        // 重新处理每个来源
        for (Source source : remainingSources) {
            try {
                if ("text".equals(source.getFileType())) {
                    // 文本来源
                    if (source.getContent() != null) {
                        Document document = Document.from(source.getContent(),
                                Metadata.from("sourceId", source.getId().toString()));
                        processDocumentSegments(source, document, notebookId);
                    }
                } else {
                    // 文件来源
                    if (source.getFilePath() != null) {
                        Path filePath = Paths.get(source.getFilePath());
                        if (Files.exists(filePath)) {
                            processDocument(source, filePath);
                        }
                    }
                }
                this.updateById(source);
            } catch (Exception e) {
                log.error("重建向量存储时处理来源失败: sourceId={}", source.getId(), e);
            }
        }

        // 持久化
        vectorStoreConfig.persistStore(notebookId);
    }

    /**
     * 更新笔记本的来源数量冗余字段
     */
    private void updateNotebookSourceCount(Long notebookId) {
        QueryWrapper queryWrapper = QueryWrapper.create()
                .eq("notebookId", notebookId);
        long count = this.count(queryWrapper);
        Notebook notebook = new Notebook();
        notebook.setId(notebookId);
        notebook.setSourceCount((int) count);
        notebookService.updateById(notebook);
    }
}
