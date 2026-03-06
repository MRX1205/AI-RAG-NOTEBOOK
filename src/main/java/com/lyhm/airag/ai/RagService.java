package com.lyhm.airag.ai;

import com.lyhm.airag.config.VectorStoreConfig;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.filter.Filter;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

/**
 * RAG 核心检索服务
 * <p>
 * 负责从向量存储中检索与用户查询相关的文档片段。
 * 是 AI 对话、报告生成、测验生成等功能的核心依赖。
 * </p>
 * <p>
 * 参考：langchain4j-examples-main/rag-examples/src/main/java/_3_advanced/
 *       _05_Advanced_RAG_with_Metadata_Filtering_Examples.java
 * 使用 Filter API 在检索时过滤，确保 top-K 结果全部来自用户勾选的来源。
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Slf4j
@Service
public class RagService {

    @Resource
    private EmbeddingModel embeddingModel;

    @Resource
    private VectorStoreConfig vectorStoreConfig;

    /**
     * 检索与查询相关的文档片段
     * <p>
     * 当 sourceIds 不为空时，使用 LangChain4j Filter API 在检索阶段过滤，
     * 而非检索后再手动过滤。这样可保证 top-K 结果全来自选中来源，
     * 不会因为后置过滤导致结果为空。
     * </p>
     *
     * @param notebookId 笔记本 ID
     * @param query      用户查询文本
     * @param sourceIds  可选的来源 ID 过滤（null 或空表示使用全部来源）
     * @return 相关文档片段的文本列表
     */
    public List<String> retrieveRelevantContent(Long notebookId, String query, List<Long> sourceIds) {
        InMemoryEmbeddingStore<TextSegment> store = vectorStoreConfig.getOrCreateStore(notebookId);

        // 构建内容检索器
        // 参考：langchain4j-examples-main/rag-examples/_3_advanced/_05_Advanced_RAG_with_Metadata_Filtering_Examples.java
        EmbeddingStoreContentRetriever.EmbeddingStoreContentRetrieverBuilder builder = EmbeddingStoreContentRetriever
                .builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)
                .maxResults(5)
                .minScore(0.5);

        // 若指定了来源 ID，在检索时加入 Filter，确保只从选中来源中取 top-K
        if (sourceIds != null && !sourceIds.isEmpty()) {
            List<String> sourceIdStrings = sourceIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
            Filter sourceFilter = metadataKey("sourceId").isIn(sourceIdStrings);
            builder.filter(sourceFilter);
            log.debug("笔记本 {} 启用来源过滤, sourceIds={}", notebookId, sourceIdStrings);
        }

        ContentRetriever retriever = builder.build();

        // 执行检索
        List<Content> contents = retriever.retrieve(Query.from(query));

        if (contents.isEmpty()) {
            log.info("笔记本 {} 未找到与查询相关的内容: {}", notebookId, query);
            return Collections.emptyList();
        }

        List<String> results = contents.stream()
                .map(content -> content.textSegment().text())
                .collect(Collectors.toList());

        log.info("笔记本 {} 检索到 {} 个相关片段", notebookId, results.size());
        return results;
    }

    /**
     * 将检索到的文档片段格式化为带编号的上下文文本
     * <p>
     * 格式：
     * [1] 片段内容...
     * [2] 片段内容...
     * </p>
     *
     * @param notebookId 笔记本 ID
     * @param query      用户查询
     * @param sourceIds  来源 ID 过滤
     * @return 格式化后的上下文文本
     */
    public String retrieveFormattedContext(Long notebookId, String query, List<Long> sourceIds) {
        List<String> contents = retrieveRelevantContent(notebookId, query, sourceIds);
        if (contents.isEmpty()) {
            return "（未找到相关文档片段）";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < contents.size(); i++) {
            sb.append("[").append(i + 1).append("] ").append(contents.get(i)).append("\n\n");
        }
        return sb.toString();
    }
}
