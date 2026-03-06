package com.lyhm.airag.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 向量存储管理器
 * <p>
 * 管理每个笔记本的 InMemoryEmbeddingStore 实例。
 * 每个笔记本拥有独立的向量存储，通过 JSON 文件实现持久化。
 * </p>
 * <p>
 * 设计思路（参考 ragdocs 文档）：
 * - 开发阶段使用 InMemoryEmbeddingStore（内存向量存储）
 * - 通过 JSON 序列化/反序列化实现持久化
 * - 每个笔记本维护一个独立的向量存储文件
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Slf4j
@Component
public class VectorStoreConfig {

    /**
     * 向量存储文件的存放目录
     */
    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    /**
     * 笔记本ID → 向量存储实例的映射
     * 使用 ConcurrentHashMap 保证线程安全
     */
    private final ConcurrentHashMap<Long, InMemoryEmbeddingStore<TextSegment>> storeMap = new ConcurrentHashMap<>();

    /**
     * 获取或创建指定笔记本的向量存储
     * <p>
     * 如果内存中已存在，直接返回。
     * 如果内存中不存在但 JSON 文件存在，从文件加载。
     * 如果都不存在，创建一个新的空存储。
     * </p>
     *
     * @param notebookId 笔记本 ID
     * @return 该笔记本的向量存储实例
     */
    public InMemoryEmbeddingStore<TextSegment> getOrCreateStore(Long notebookId) {
        return storeMap.computeIfAbsent(notebookId, id -> {
            // 尝试从文件加载
            Path storePath = getStorePath(id);
            if (Files.exists(storePath)) {
                try {
                    String json = Files.readString(storePath);
                    log.info("从文件加载笔记本 {} 的向量存储: {}", id, storePath);
                    return InMemoryEmbeddingStore.fromJson(json);
                } catch (Exception e) {
                    log.error("加载向量存储文件失败，将创建新的存储: {}", storePath, e);
                }
            }
            log.info("创建笔记本 {} 的新向量存储", id);
            return new InMemoryEmbeddingStore<>();
        });
    }

    /**
     * 持久化指定笔记本的向量存储到 JSON 文件
     *
     * @param notebookId 笔记本 ID
     */
    public void persistStore(Long notebookId) {
        InMemoryEmbeddingStore<TextSegment> store = storeMap.get(notebookId);
        if (store == null) {
            return;
        }
        try {
            Path storePath = getStorePath(notebookId);
            // 确保目录存在
            Files.createDirectories(storePath.getParent());
            String json = store.serializeToJson();
            Files.writeString(storePath, json);
            log.info("向量存储已持久化: {}", storePath);
        } catch (IOException e) {
            log.error("持久化向量存储失败: notebookId={}", notebookId, e);
        }
    }

    /**
     * 删除指定笔记本的向量存储（内存 + 文件）
     *
     * @param notebookId 笔记本 ID
     */
    public void removeStore(Long notebookId) {
        storeMap.remove(notebookId);
        Path storePath = getStorePath(notebookId);
        try {
            Files.deleteIfExists(storePath);
            log.info("已删除笔记本 {} 的向量存储文件", notebookId);
        } catch (IOException e) {
            log.error("删除向量存储文件失败: notebookId={}", notebookId, e);
        }
    }

    /**
     * 应用关闭时，持久化所有向量存储
     */
    @PreDestroy
    public void persistAll() {
        log.info("应用关闭，持久化所有向量存储...");
        storeMap.forEach((notebookId, store) -> persistStore(notebookId));
        log.info("所有向量存储持久化完成，共 {} 个", storeMap.size());
    }

    /**
     * 获取向量存储 JSON 文件的路径
     */
    private Path getStorePath(Long notebookId) {
        return Paths.get(uploadDir, "vector-store", "notebook_" + notebookId + ".json");
    }
}
