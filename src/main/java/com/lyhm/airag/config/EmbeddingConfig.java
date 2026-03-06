package com.lyhm.airag.config;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiEmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Embedding 模型配置
 * <p>
 * 使用阿里云百炼 DashScope 的 text-embedding-v3 模型，通过其 OpenAI 兼容接口调用。
 * </p>
 * <p>
 * 技术选型说明：
 * - 原方案：QwenEmbeddingModel（langchain4j-community-dashscope）内部使用 OkHttp，
 *   在 VPN/企业代理环境下存在 SSL 握手失败问题（PKIX path building failed）。
 * - 现方案：OpenAiEmbeddingModel（langchain4j-open-ai）使用 Spring RestClient，
 *   与 DeepSeek 聊天模型使用相同的 HTTP 客户端，已验证在当前环境正常工作。
 * - DashScope 提供 OpenAI 兼容 API：https://dashscope.aliyuncs.com/compatible-mode/v1
 *   模型名称 text-embedding-v3 在该接口下完全兼容。
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Configuration
public class EmbeddingConfig {

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Value("${dashscope.embedding.model-name}")
    private String modelName;

    /**
     * 创建 Embedding 模型 Bean
     * <p>
     * 通过 DashScope 的 OpenAI 兼容接口调用 text-embedding-v3 模型，
     * 使用 Spring RestClient 作为 HTTP 客户端，规避 OkHttp 的 SSL 兼容性问题。
     * </p>
     */
    @Bean
    public EmbeddingModel embeddingModel() {
        return OpenAiEmbeddingModel.builder()
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}
