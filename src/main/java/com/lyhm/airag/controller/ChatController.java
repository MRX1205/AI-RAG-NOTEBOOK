package com.lyhm.airag.controller;

import com.lyhm.airag.ai.RagService;
import com.lyhm.airag.common.BaseResponse;
import com.lyhm.airag.common.ResultUtils;
import com.lyhm.airag.exception.ErrorCode;
import com.lyhm.airag.exception.ThrowUtils;
import com.lyhm.airag.model.entity.Notebook;
import com.lyhm.airag.model.entity.User;
import com.lyhm.airag.service.NotebookService;
import com.lyhm.airag.service.UserService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * AI 对话控制器
 * <p>
 * 提供基于 RAG 的 AI 对话接口，支持 SSE 流式输出。
 * 工作流程：
 * 1. 根据用户问题从向量存储检索相关文档片段
 * 2. 将文档片段作为上下文构建 Prompt
 * 3. 调用 LLM 生成回答（流式输出）
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Slf4j
@RestController
@RequestMapping("/chat")
public class ChatController {

    @Resource
    private RagService ragService;

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel streamingChatModel;

    @Resource
    private NotebookService notebookService;

    @Resource
    private UserService userService;

    // 用于处理 SSE 流式输出的线程池，避免无限制创建 new Thread()
    private static final ExecutorService SSE_EXECUTOR = Executors.newCachedThreadPool();

    /**
     * RAG 对话 — SSE 流式输出
     * <p>
     * 前端使用 EventSource API 接收流式数据。
     * 每个 SSE 事件包含一个文本片段，最后发送 [DONE] 标志。
     * </p>
     *
     * @param notebookId 笔记本 ID
     * @param message    用户消息
     * @param sourceIds  选中的来源 ID（逗号分隔，可选）
     * @param request    HTTP 请求
     * @return SSE 事件流
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(
            @RequestParam Long notebookId,
            @RequestParam String message,
            @RequestParam(required = false) String sourceIds,
            HttpServletRequest request) {

        // 1. 参数校验
        ThrowUtils.throwIf(notebookId == null || notebookId <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(message == null || message.trim().isEmpty(), ErrorCode.PARAMS_ERROR, "消息不能为空");

        // 2. 获取登录用户并校验笔记本归属
        User loginUser = userService.getLoginUser(request);
        Notebook notebook = notebookService.getById(notebookId);
        ThrowUtils.throwIf(notebook == null, ErrorCode.NOT_FOUND_ERROR, "笔记本不存在");
        ThrowUtils.throwIf(!notebook.getUserId().equals(loginUser.getId()),
                ErrorCode.NO_AUTH_ERROR, "无权访问该笔记本");

        // 3. 解析来源 ID（加入异常处理，防止非法字符导致 500）
        List<Long> sourceIdList = null;
        if (sourceIds != null && !sourceIds.isEmpty()) {
            try {
                sourceIdList = Arrays.stream(sourceIds.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .map(Long::parseLong)
                        .collect(Collectors.toList());
            } catch (NumberFormatException e) {
                throw new com.lyhm.airag.exception.BusinessException(
                        com.lyhm.airag.exception.ErrorCode.PARAMS_ERROR, "来源 ID 格式错误");
            }
        }

        // 4. 检索相关文档片段
        String context = ragService.retrieveFormattedContext(notebookId, message, sourceIdList);

        // 5. 构建完整 Prompt
        String systemPrompt = buildSystemPrompt(context);
        String userMessage = message;

        // 6. 创建 SSE Emitter
        SseEmitter emitter = new SseEmitter(120000L); // 2 分钟超时

        // 7. 异步调用 LLM 流式生成（使用线程池，而非每次 new Thread()）
        SSE_EXECUTOR.submit(() -> {
            try {
                // 使用 LangChain4j 构建聊天请求
                dev.langchain4j.data.message.SystemMessage sysMsg = dev.langchain4j.data.message.SystemMessage
                        .from(systemPrompt);
                dev.langchain4j.data.message.UserMessage usrMsg = dev.langchain4j.data.message.UserMessage
                        .from(userMessage);

                streamingChatModel.chat(List.of(sysMsg, usrMsg),
                        new dev.langchain4j.model.chat.response.StreamingChatResponseHandler() {
                            @Override
                            public void onPartialResponse(String partialResponse) {
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
                                try {
                                    emitter.send(SseEmitter.event().data("[DONE]"));
                                    emitter.complete();
                                } catch (IOException e) {
                                    log.error("SSE 完成失败", e);
                                }
                            }

                            @Override
                            public void onError(Throwable error) {
                                log.error("LLM 调用失败", error);
                                try {
                                    emitter.send(SseEmitter.event().data("[ERROR]"));
                                    emitter.completeWithError(error);
                                } catch (IOException e) {
                                    log.error("SSE 错误发送失败", e);
                                }
                            }
                        });
            } catch (Exception e) {
                log.error("对话处理失败", e);
                try {
                    emitter.send(SseEmitter.event().data("[ERROR]"));
                    emitter.completeWithError(e);
                } catch (IOException ex) {
                    log.error("SSE 错误处理失败", ex);
                }
            }
        });

        return emitter;
    }

    /**
     * 获取推荐问题
     * <p>
     * 根据笔记本内容生成推荐的提问方向
     * </p>
     */
    @GetMapping("/suggestions")
    public BaseResponse<List<String>> getSuggestions(
            @RequestParam Long notebookId,
            HttpServletRequest request) {

        User loginUser = userService.getLoginUser(request);
        Notebook notebook = notebookService.getById(notebookId);
        ThrowUtils.throwIf(notebook == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!notebook.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR);

        // 检索一些文档片段来生成推荐问题
        String context = ragService.retrieveFormattedContext(notebookId, "总结文档主要内容", null);

        if (context.contains("未找到")) {
            return ResultUtils.success(List.of("请先上传来源文档，然后开始提问"));
        }

        // 调用 LLM 生成推荐问题
        String prompt = "根据以下文档内容，生成3个用户可能感兴趣的问题。只返回问题列表，每行一个问题，不要序号和其他格式。\n\n文档内容：\n" + context;
        try {
            ChatResponse response = chatModel.chat(
                    ChatRequest.builder()
                            .messages(List.of(dev.langchain4j.data.message.UserMessage.from(prompt)))
                            .build());
            String result = response.aiMessage().text();
            List<String> suggestions = Arrays.stream(result.split("\n"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .limit(3)
                    .collect(Collectors.toList());
            return ResultUtils.success(suggestions);
        } catch (Exception e) {
            log.error("生成推荐问题失败", e);
            return ResultUtils.success(List.of("这份文档讲了什么？", "请总结主要内容", "有哪些关键知识点？"));
        }
    }

    /**
     * 构建系统 Prompt（包含 RAG 检索到的文档片段作为上下文）
     */
    private String buildSystemPrompt(String context) {
        return """
                你是一个专业的智能知识助手 NaviCat AI。你的核心任务是协助用户管理笔记本中的私有知识库。

                你的行为准则如下：
                1. **严格以源为中心**：你必须仅根据提供的"文档片段（Sources/Context）"来回答用户的问题。如果提供的资料中不包含相关答案，请直接告知用户"在您的笔记本资料中没有找到相关信息"，严禁胡编乱造或使用模型自带的外部知识。
                2. **强制引用标注**：在回答每一个关键观点、事实或结论时，必须在文末或句末添加引用的角标。格式要求：使用 [n] 的形式，其中 n 是对应文档片段的编号。
                3. **结构化整理**：当用户要求你进行"总结"、"摘要"或"知识梳理"时，请使用清晰的 Markdown 标题、加粗文字和列表。
                4. **溯源导向**：你的目标是让用户相信每一个答案都是有据可依的。如果文档中有冲突的信息，请同时列出并说明来源。
                5. **对话语气**：保持客观、严谨、简洁且富有启发性，像一名专业的学术助教。

                当前笔记本上下文如下：
                """
                + context;
    }
}
