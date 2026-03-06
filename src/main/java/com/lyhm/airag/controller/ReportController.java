package com.lyhm.airag.controller;

import com.lyhm.airag.ai.RagService;
import com.lyhm.airag.common.BaseResponse;
import com.lyhm.airag.common.DeleteRequest;
import com.lyhm.airag.common.ResultUtils;
import com.lyhm.airag.exception.ErrorCode;
import com.lyhm.airag.exception.ThrowUtils;
import com.lyhm.airag.model.dto.report.ReportGenerateRequest;
import com.lyhm.airag.model.entity.Notebook;
import com.lyhm.airag.model.entity.Report;
import com.lyhm.airag.model.entity.User;
import com.lyhm.airag.model.enums.ReportTypeEnum;
import com.lyhm.airag.model.vo.ReportVO;
import com.lyhm.airag.service.NotebookService;
import com.lyhm.airag.service.ReportService;
import com.lyhm.airag.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 报告控制器
 * <p>
 * 提供报告生成（SSE 流式）、列表、详情、删除接口。
 * 支持 5 种报告类型：简报、学习指南、FAQ、时间线、自定义。
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Slf4j
@RestController
@RequestMapping("/report")
public class ReportController {

    @Resource
    private ReportService reportService;
    @Resource
    private RagService ragService;
    @Resource
    private StreamingChatModel streamingChatModel;
    @Resource
    private NotebookService notebookService;
    @Resource
    private UserService userService;

    // 用于处理报告生成 SSE 流的线程池
    private static final ExecutorService SSE_EXECUTOR = Executors.newCachedThreadPool();

    /**
     * 生成报告（SSE 流式输出）
     */
    @PostMapping(value = "/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter generateReport(
            @RequestBody ReportGenerateRequest generateRequest,
            HttpServletRequest request) {

        // 参数校验
        ThrowUtils.throwIf(generateRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(generateRequest.getNotebookId() == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(generateRequest.getReportType() == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(ReportTypeEnum.getEnumByValue(generateRequest.getReportType()) == null,
                ErrorCode.PARAMS_ERROR, "不支持的报告类型");

        User loginUser = userService.getLoginUser(request);
        Long notebookId = generateRequest.getNotebookId();
        checkNotebookOwnership(notebookId, loginUser.getId());

        // 检索相关文档
        String context = ragService.retrieveFormattedContext(
                notebookId, "全面总结所有文档要点", generateRequest.getSourceIds());

        // 构建报告生成 Prompt
        String prompt = buildReportPrompt(generateRequest.getReportType(),
                generateRequest.getCustomPrompt(), context);

        // SSE Emitter
        SseEmitter emitter = new SseEmitter(180000L);
        StringBuilder fullContent = new StringBuilder();

        SSE_EXECUTOR.submit(() -> {
            try {
                dev.langchain4j.data.message.UserMessage usrMsg = dev.langchain4j.data.message.UserMessage.from(prompt);

                streamingChatModel.chat(List.of(usrMsg),
                        new dev.langchain4j.model.chat.response.StreamingChatResponseHandler() {
                            @Override
                            public void onPartialResponse(String partialResponse) {
                                if (partialResponse != null && !partialResponse.isEmpty()) {
                                    fullContent.append(partialResponse);
                                    try {
                                        emitter.send(SseEmitter.event().data(partialResponse));
                                    } catch (IOException e) {
                                        log.error("SSE 发送失败", e);
                                    }
                                }
                            }

                            @Override
                            public void onCompleteResponse(
                                    dev.langchain4j.model.chat.response.ChatResponse completeResponse) {
                                // 保存报告到数据库
                                try {
                                    Report report = Report.builder()
                                            .notebookId(notebookId)
                                            .userId(loginUser.getId())
                                            .title(generateReportTitle(generateRequest.getReportType()))
                                            .reportType(generateRequest.getReportType())
                                            .customPrompt(generateRequest.getCustomPrompt())
                                            .content(fullContent.toString())
                                            .sourceIds(
                                                    generateRequest.getSourceIds() != null
                                                            ? generateRequest.getSourceIds().stream()
                                                                    .map(String::valueOf)
                                                                    .collect(Collectors.joining(","))
                                                            : null)
                                            .build();
                                    reportService.save(report);
                                    emitter.send(SseEmitter.event().data("[DONE:" + report.getId() + "]"));
                                    emitter.complete();
                                } catch (Exception e) {
                                    log.error("保存报告失败", e);
                                    try {
                                        emitter.completeWithError(e);
                                    } catch (Exception ignored) {
                                    }
                                }
                            }

                            @Override
                            public void onError(Throwable error) {
                                log.error("报告生成失败", error);
                                try {
                                    emitter.send(SseEmitter.event().data("[ERROR]"));
                                    emitter.completeWithError(error);
                                } catch (IOException e) {
                                    log.error("SSE 错误发送失败", e);
                                }
                            }
                        });
            } catch (Exception e) {
                log.error("报告生成异常", e);
                try {
                    emitter.completeWithError(e);
                } catch (Exception ignored) {
                }
            }
        });

        return emitter;
    }

    /** 报告列表 */
    @GetMapping("/list")
    public BaseResponse<List<ReportVO>> listReports(
            @RequestParam Long notebookId,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        checkNotebookOwnership(notebookId, loginUser.getId());
        QueryWrapper qw = QueryWrapper.create()
                .eq("notebookId", notebookId)
                .orderBy("createTime", false);
        return ResultUtils.success(reportService.getReportVOList(reportService.list(qw)));
    }

    /** 报告详情 */
    @GetMapping("/get")
    public BaseResponse<ReportVO> getReport(
            @RequestParam Long id,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Report report = reportService.getById(id);
        ThrowUtils.throwIf(report == null, ErrorCode.NOT_FOUND_ERROR);
        checkNotebookOwnership(report.getNotebookId(), loginUser.getId());
        return ResultUtils.success(reportService.getReportVO(report));
    }

    /** 删除报告 */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteReport(
            @RequestBody DeleteRequest deleteRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Report report = reportService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(report == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!report.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR);
        reportService.removeById(deleteRequest.getId());
        return ResultUtils.success(true);
    }

    private void checkNotebookOwnership(Long notebookId, Long userId) {
        Notebook notebook = notebookService.getById(notebookId);
        ThrowUtils.throwIf(notebook == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!notebook.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR);
    }

    private String generateReportTitle(String reportType) {
        return switch (reportType) {
            case "briefing" -> "简报文档";
            case "study_guide" -> "学习指南";
            case "faq" -> "常见问题";
            case "timeline" -> "时间线";
            case "custom" -> "自定义报告";
            default -> "报告";
        };
    }

    private String buildReportPrompt(String reportType, String customPrompt, String context) {
        String typeInstruction = switch (reportType) {
            case "briefing" ->
                "请基于以下文档内容，生成一份专业的简报文档。要求：\n1. 包含执行摘要\n2. 列出核心要点（每个要点配有详细说明）\n3. 给出结论和建议\n使用 Markdown 格式排版。";
            case "study_guide" ->
                "请基于以下文档内容，生成一份全面的学习指南。要求：\n1. 概述学习目标\n2. 按主题/章节组织知识点\n3. 每个知识点给出关键定义和解释\n4. 在末尾添加重点回顾清单\n使用 Markdown 格式排版。";
            case "faq" ->
                "请基于以下文档内容，生成一份 FAQ（常见问题解答）文档。要求：\n1. 提取文档中最重要的 8-12 个问题\n2. 每个问题给出简洁准确的回答\n3. 回答直接引用文档原文\n使用 Markdown 格式排版，用 ## 标记每个问题。";
            case "timeline" ->
                "请基于以下文档内容，生成一份时间线文档。要求：\n1. 按时间顺序梳理文档中提到的关键事件\n2. 每个事件标注时间、描述和影响\n3. 如果文档没有明确时间，则按逻辑顺序排列\n使用 Markdown 格式排版。";
            case "custom" ->
                "请根据以下指令，基于文档内容生成报告：\n" + (customPrompt != null ? customPrompt : "请总结文档主要内容") + "\n使用 Markdown 格式排版。";
            default -> "请总结以下文档内容，使用 Markdown 格式排版。";
        };
        return typeInstruction + "\n\n文档内容：\n" + context;
    }
}
