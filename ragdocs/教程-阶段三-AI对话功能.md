# 阶段三教程：AI 对话功能（RAG 检索 + SSE 流式输出）

> 本教程将教你实现 RAG 对话功能：用户提问 → 从向量存储中检索相关文档片段 → 作为上下文发给 LLM → 以 SSE 流式方式返回答案。

---

## 1. 这个功能完成了什么

完成后，系统具备以下能力：
- ✅ 用户在笔记本中**提问**，AI 基于上传的文档内容**回答**
- ✅ 回答**严格引用文档**，用 `[n]` 标注来源编号
- ✅ **流式输出**——AI 的回答像打字机一样逐字出现，而不是等全部生成完才显示
- ✅ 每次回答后，AI 自动生成**推荐问题**供用户继续探索
- ✅ 用户可以**勾选来源**，限定 AI 只参考特定来源

## 2. 为什么这么做

### 2.1 为什么需要 SSE 流式输出？

LLM 生成一个完整回答可能需要 5-30 秒。如果等全部生成完再显示，用户体验很差（感觉"卡住了"）。SSE（Server-Sent Events）让服务端可以**一段一段地**把回答推送到前端，实现打字机效果。

```
传统方式：用户提问 → 等待 10 秒 → 一次性显示全部答案
SSE 方式：用户提问 → 0.5秒后开始逐字显示 → 边生成边显示
```

### 2.2 为什么用 GET 而不是 POST？

SSE 使用前端的 `EventSource` API，它只支持 GET 请求。所以我们把参数放在 URL query string 中。

### 2.3 为什么需要推荐问题？

很多用户面对一个空输入框时不知道该问什么。推荐问题可以引导用户开始探索文档内容。

---

## 3. 开始教程

### 3.1 创建 RAG 检索服务（RagService.java）

> **这是整个系统最核心的类**——它连接了向量存储和 LLM，是 RAG 的"桥梁"。

**文件位置**：`src/main/java/com/lyhm/airag/ai/RagService.java`

```java
package com.lyhm.airag.ai;

import com.lyhm.airag.config.VectorStoreConfig;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RagService {

    @Resource
    private EmbeddingModel embeddingModel;  // DashScope 嵌入模型

    @Resource
    private VectorStoreConfig vectorStoreConfig;  // 向量存储管理器
```

**核心方法 1：检索相关文档片段**

```java
    /**
     * 检索与用户查询最相关的文档片段
     *
     * 工作原理：
     * 1. 获取笔记本对应的向量存储
     * 2. 构建 EmbeddingStoreContentRetriever（LangChain4j 的检索器）
     * 3. 把用户问题转为向量，在存储中找最相似的 5 个片段
     * 4. 可选：按 sourceIds 过滤，只返回指定来源的片段
     *
     * @param notebookId 笔记本 ID
     * @param query      用户的问题文本
     * @param sourceIds  可选的来源 ID 列表（null 表示搜索全部来源）
     * @return 相关文档片段的文本列表
     */
    public List<String> retrieveRelevantContent(Long notebookId, String query, 
                                                 List<Long> sourceIds) {
        // 1. 获取该笔记本的向量存储
        InMemoryEmbeddingStore<TextSegment> store = 
                vectorStoreConfig.getOrCreateStore(notebookId);

        // 2. 构建检索器
        //    maxResults(5)  — 最多返回 5 个最相关的片段
        //    minScore(0.5)  — 相似度低于 0.5 的不要（避免返回不相关内容）
        ContentRetriever retriever = EmbeddingStoreContentRetriever.builder()
                .embeddingStore(store)
                .embeddingModel(embeddingModel)  // 用同一个模型把问题也转为向量
                .maxResults(5)
                .minScore(0.5)
                .build();

        // 3. 执行检索
        //    内部流程：query文本 → 向量 → 在store中计算余弦相似度 → 返回top5
        List<Content> contents = retriever.retrieve(Query.from(query));

        if (contents.isEmpty()) {
            log.info("笔记本 {} 未找到与查询相关的内容: {}", notebookId, query);
            return Collections.emptyList();
        }

        // 4. 如果用户勾选了特定来源，只保留这些来源的片段
        List<String> results;
        if (sourceIds != null && !sourceIds.isEmpty()) {
            List<String> sourceIdStrings = sourceIds.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());
            results = contents.stream()
                    .filter(content -> {
                        // 检查片段的 sourceId 元数据是否匹配
                        String contentSourceId = content.textSegment()
                                .metadata().getString("sourceId");
                        return contentSourceId != null && 
                               sourceIdStrings.contains(contentSourceId);
                    })
                    .map(content -> content.textSegment().text())
                    .collect(Collectors.toList());
        } else {
            results = contents.stream()
                    .map(content -> content.textSegment().text())
                    .collect(Collectors.toList());
        }

        log.info("笔记本 {} 检索到 {} 个相关片段", notebookId, results.size());
        return results;
    }
```

**核心方法 2：格式化上下文（给 LLM 用）**

```java
    /**
     * 将检索到的片段格式化为带编号的上下文文本
     *
     * 输出格式：
     * [1] 第一个相关片段的内容...
     *
     * [2] 第二个相关片段的内容...
     *
     * 这个编号很重要！它让 LLM 在回答中可以引用 [1]、[2] 来标注来源
     */
    public String retrieveFormattedContext(Long notebookId, String query, 
                                           List<Long> sourceIds) {
        List<String> contents = retrieveRelevantContent(notebookId, query, sourceIds);
        
        if (contents.isEmpty()) {
            return "（未找到相关文档片段）";
        }
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < contents.size(); i++) {
            sb.append("[").append(i + 1).append("] ")
              .append(contents.get(i))
              .append("\n\n");
        }
        return sb.toString();
    }
}
```

**理解关键概念**：
- `EmbeddingStoreContentRetriever` — LangChain4j 的核心组件，负责"用向量相似度搜索"
- `maxResults(5)` — 只取最相关的 5 个片段，避免上下文过长
- `minScore(0.5)` — 过滤掉不太相关的结果，提高答案质量
- 带编号的格式 `[1]...[2]...` — 让 LLM 可以在回答中标注引用来源

---

### 3.2 创建对话控制器（ChatController.java）

> **SSE 流式输出是本阶段的技术重点**

**文件位置**：`src/main/java/com/lyhm/airag/controller/ChatController.java`

#### 3.2.1 SSE 流式对话接口

```java
@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    private RagService ragService;

    @Resource
    private ChatModel chatModel;  // 普通（非流式）对话模型

    @Resource
    private StreamingChatModel streamingChatModel;  // 流式对话模型

    @Resource
    private NotebookService notebookService;

    @Resource
    private UserService userService;

    /**
     * SSE 流式对话接口
     * 
     * 流程：
     * 1. 参数校验 + 权限校验
     * 2. 从向量存储检索相关文档片段
     * 3. 构建包含上下文的系统 Prompt
     * 4. 创建 SseEmitter，开线程调用流式 LLM
     * 5. LLM 每生成一段文字，通过 SSE 推送到前端
     * 6. 生成完毕，发送 [DONE] 信号
     *
     * produces = TEXT_EVENT_STREAM_VALUE 告诉 Spring 这是 SSE 接口
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @RequestParam Long notebookId,
            @RequestParam String message,
            @RequestParam(required = false) String sourceIds,
            HttpServletRequest request) {

        // 1. 参数校验
        ThrowUtils.throwIf(notebookId == null || notebookId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(message == null || message.trim().isEmpty(), 
                ErrorCode.PARAMS_ERROR, "消息不能为空");

        // 2. 权限校验
        User loginUser = userService.getLoginUser(request);
        Notebook notebook = notebookService.getById(notebookId);
        ThrowUtils.throwIf(notebook == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!notebook.getUserId().equals(loginUser.getId()), 
                ErrorCode.NO_AUTH_ERROR);

        // 3. 解析前端传过来的来源 ID（逗号分隔的字符串 → Long 列表）
        List<Long> sourceIdList = null;
        if (sourceIds != null && !sourceIds.isEmpty()) {
            sourceIdList = Arrays.stream(sourceIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        }

        // 4. 【RAG 核心】检索相关文档片段
        String context = ragService.retrieveFormattedContext(
                notebookId, message, sourceIdList);

        // 5. 构建系统 Prompt（把文档片段嵌入到 Prompt 中）
        String systemPrompt = buildSystemPrompt(context);

        // 6. 创建 SSE Emitter（2 分钟超时）
        SseEmitter emitter = new SseEmitter(120000L);

        // 7. 开新线程异步调用 LLM（避免阻塞主线程）
        new Thread(() -> {
            try {
                // 构建消息列表：系统消息 + 用户消息
                var sysMsg = dev.langchain4j.data.message.SystemMessage.from(systemPrompt);
                var usrMsg = dev.langchain4j.data.message.UserMessage.from(message);

                // 调用流式 LLM
                streamingChatModel.chat(
                    List.of(sysMsg, usrMsg),
                    new StreamingChatResponseHandler() {
                        
                        @Override
                        public void onPartialResponse(String partialResponse) {
                            // LLM 每生成一小段文字，就通过 SSE 推送到前端
                            if (partialResponse != null && !partialResponse.isEmpty()) {
                                try {
                                    emitter.send(SseEmitter.event().data(partialResponse));
                                } catch (IOException e) {
                                    log.error("SSE 发送失败", e);
                                }
                            }
                        }

                        @Override
                        public void onCompleteResponse(ChatResponse completeResponse) {
                            // LLM 生成完毕，发送完成信号
                            try {
                                emitter.send(SseEmitter.event().data("[DONE]"));
                                emitter.complete();
                            } catch (IOException e) {
                                log.error("SSE 完成失败", e);
                            }
                        }

                        @Override
                        public void onError(Throwable error) {
                            // LLM 调用出错
                            log.error("LLM 调用失败", error);
                            try {
                                emitter.send(SseEmitter.event().data("[ERROR]"));
                                emitter.completeWithError(error);
                            } catch (IOException e) {
                                log.error("SSE 错误发送失败", e);
                            }
                        }
                    }
                );
            } catch (Exception e) {
                log.error("对话处理失败", e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ex) {}
            }
        }).start();

        return emitter;
    }
```

**理解 StreamingChatResponseHandler 的三个回调**：

```
LLM 开始生成 → onPartialResponse("你")
                → onPartialResponse("好")
                → onPartialResponse("，")
                → onPartialResponse("根据")
                → onPartialResponse("文档")
                → ...（持续推送）
                → onCompleteResponse(完整响应)
```

#### 3.2.2 推荐问题接口

```java
    /**
     * 获取推荐问题
     * 
     * 策略：从文档中检索一些内容，让 LLM 基于内容生成 3 个可能的问题
     */
    @GetMapping("/suggestions")
    public BaseResponse<List<String>> getSuggestions(
            @RequestParam Long notebookId,
            HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        // ... 权限校验 ...

        // 检索文档片段
        String context = ragService.retrieveFormattedContext(
                notebookId, "总结文档主要内容", null);

        if (context.contains("未找到")) {
            return ResultUtils.success(List.of("请先上传来源文档，然后开始提问"));
        }

        // 用 LLM 生成推荐问题
        String prompt = "根据以下文档内容，生成3个用户可能感兴趣的问题。" +
                "只返回问题列表，每行一个。\n\n文档内容：\n" + context;
        try {
            ChatResponse response = chatModel.chat(
                    ChatRequest.builder()
                            .messages(List.of(UserMessage.from(prompt)))
                            .build());
            
            // 解析返回结果（按换行符分割）
            List<String> suggestions = Arrays.stream(
                    response.aiMessage().text().split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .limit(3)
                    .collect(Collectors.toList());
            return ResultUtils.success(suggestions);
        } catch (Exception e) {
            // 降级：LLM 失败时返回默认问题
            return ResultUtils.success(List.of(
                    "这份文档讲了什么？",
                    "请总结主要内容",
                    "有哪些关键知识点？"
            ));
        }
    }
```

#### 3.2.3 系统 Prompt 设计

```java
    private String buildSystemPrompt(String context) {
        return """
            你是一个专业的智能知识助手 NaviCat AI。
            
            行为准则：
            1. 严格以源为中心：只根据提供的文档片段回答，不要编造
            2. 强制引用标注：用 [n] 标注每个观点的来源
            3. 结构化整理：使用 Markdown 格式
            4. 溯源导向：让用户相信答案有据可依
            5. 对话语气：客观、严谨、简洁
            
            当前笔记本上下文如下：
            """ + context;
    }
```

**Prompt 设计的关键**：
- **严格以源为中心** — 防止 LLM 使用自身训练数据编造答案
- **强制引用标注 [n]** — 让用户可以追溯信息来源
- **结构化整理** — Markdown 格式让答案更易读

---

### 3.3 前端实现

#### 3.3.1 chatController.ts

```typescript
/**
 * 构建 SSE 流式对话的 URL
 * 
 * 因为 EventSource 只支持 GET 请求，
 * 所以参数通过 URL query string 传递
 */
export function getChatStreamUrl(
    notebookId: number, message: string, sourceIds?: number[]) {
  let url = `/api/chat/stream?notebookId=${notebookId}` +
            `&message=${encodeURIComponent(message)}`
  if (sourceIds && sourceIds.length > 0) {
    url += `&sourceIds=${sourceIds.join(',')}`
  }
  return url
}
```

#### 3.3.2 ChatPanel.vue — SSE 接收核心代码

```typescript
/** 发送消息并接收流式回答 */
const sendMessage = async (text?: string) => {
  const msg = text || inputText.value.trim()
  if (!msg || isGenerating.value) return

  // 添加用户消息气泡
  messages.value.push({ role: 'user', content: msg })
  // 添加 AI 占位气泡（显示打字动画）
  messages.value.push({ role: 'assistant', content: '', loading: true })
  isGenerating.value = true

  // 创建 EventSource 连接 SSE
  const url = getChatStreamUrl(props.notebookId, msg, props.selectedSourceIds)
  const eventSource = new EventSource(url)
  const aiMsgIdx = messages.value.length - 1

  // 每收到一段文字，追加到 AI 消息中
  eventSource.onmessage = (event) => {
    const data = event.data

    if (data === '[DONE]') {
      // 生成完毕
      eventSource.close()
      messages.value[aiMsgIdx].loading = false
      isGenerating.value = false
      loadSuggestions()  // 加载推荐问题
      return
    }

    if (data === '[ERROR]') {
      eventSource.close()
      messages.value[aiMsgIdx].content = '抱歉，AI 服务暂时不可用。'
      messages.value[aiMsgIdx].loading = false
      isGenerating.value = false
      return
    }

    // 追加文本片段（实现逐字出现效果）
    messages.value[aiMsgIdx].content += data
    scrollToBottom()
  }

  eventSource.onerror = () => {
    eventSource.close()
    messages.value[aiMsgIdx].loading = false
    isGenerating.value = false
  }
}
```

**关键点**：
- `new EventSource(url)` — 浏览器原生 SSE API，自动保持连接
- `eventSource.onmessage` — 每次服务端推送 `data:xxx\n\n` 时触发
- 逐字追加 `content += data` — 实现打字机效果
- `[DONE]` 和 `[ERROR]` 是我们自定义的控制信号

---

## 小结

本阶段实现了 RAG 对话的完整链路：

```
用户输入问题 "量子纠缠是什么？"
    ↓
ChatController 接收消息
    ↓
RagService.retrieveFormattedContext()
    ↓ 把问题转为向量 → 在向量存储中搜索 → 返回最相关的5个片段
    ↓ "[1] 量子纠缠是指两个粒子..."
    ↓ "[2] 爱因斯坦称之为..."
    ↓
构建系统 Prompt（把片段作为上下文）
    ↓
StreamingChatModel.chat() 流式调用 DeepSeek
    ↓ "根据" → "文档" → "，" → "量子纠缠" → "是指..." → "[1]"
    ↓ （每个片段通过 SSE 推送到前端）
    ↓
前端 EventSource 接收并逐字显示
```
