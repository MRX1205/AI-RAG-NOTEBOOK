package com.lyhm.airag.controller;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.lyhm.airag.ai.RagService;
import com.lyhm.airag.common.BaseResponse;
import com.lyhm.airag.common.DeleteRequest;
import com.lyhm.airag.common.ResultUtils;
import com.lyhm.airag.exception.BusinessException;
import com.lyhm.airag.exception.ErrorCode;
import com.lyhm.airag.exception.ThrowUtils;
import com.lyhm.airag.model.dto.quiz.QuizGenerateRequest;
import com.lyhm.airag.model.dto.quiz.QuizSubmitRequest;
import com.lyhm.airag.model.entity.Notebook;
import com.lyhm.airag.model.entity.Quiz;
import com.lyhm.airag.model.entity.QuizRecord;
import com.lyhm.airag.model.entity.User;
import com.lyhm.airag.model.enums.QuizDifficultyEnum;
import com.lyhm.airag.model.vo.QuizVO;
import com.lyhm.airag.service.NotebookService;
import com.lyhm.airag.service.QuizService;
import com.lyhm.airag.service.UserService;
import com.mybatisflex.core.BaseMapper;
import com.mybatisflex.core.query.QueryWrapper;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 测验控制器
 * <p>
 * 提供测验生成、答题提交、成绩查看等接口。
 * 测验题目为 JSON 格式的选择题。
 * </p>
 *
 * @author <a href="https://lyhlz.cn">掠影航猫</a>
 */
@Slf4j
@RestController
@RequestMapping("/quiz")
public class QuizController {

    @Resource
    private QuizService quizService;
    @Resource
    private RagService ragService;
    @Resource
    private ChatModel chatModel;
    @Resource
    private NotebookService notebookService;
    @Resource
    private UserService userService;
    @Resource
    private com.lyhm.airag.mapper.QuizRecordMapper quizRecordMapper;

    /**
     * 生成测验
     */
    @PostMapping("/generate")
    public BaseResponse<QuizVO> generateQuiz(
            @RequestBody QuizGenerateRequest generateRequest,
            HttpServletRequest request) {

        // 参数校验
        ThrowUtils.throwIf(generateRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(generateRequest.getNotebookId() == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(generateRequest.getQuestionCount() == null ||
                generateRequest.getQuestionCount() < 1, ErrorCode.PARAMS_ERROR, "题目数量无效");
        ThrowUtils.throwIf(QuizDifficultyEnum.getEnumByValue(generateRequest.getDifficulty()) == null,
                ErrorCode.PARAMS_ERROR, "不支持的难度级别");

        User loginUser = userService.getLoginUser(request);
        Long notebookId = generateRequest.getNotebookId();
        checkNotebookOwnership(notebookId, loginUser.getId());

        // 检索文档内容
        String context = ragService.retrieveFormattedContext(
                notebookId, "全部内容要点", generateRequest.getSourceIds());

        // 构建测验 Prompt
        String prompt = buildQuizPrompt(generateRequest.getQuestionCount(),
                generateRequest.getDifficulty(), context);

        // 调用 LLM 生成题目
        try {
            ChatResponse response = chatModel.chat(
                    ChatRequest.builder()
                            .messages(List.of(dev.langchain4j.data.message.UserMessage.from(prompt)))
                            .build());
            String questionsJson = extractJson(response.aiMessage().text());

            // 验证 JSON 格式，LLM 输出可能不规范，加入兜底处理
            JSONArray jsonArray;
            try {
                jsonArray = JSONUtil.parseArray(questionsJson);
            } catch (Exception parseEx) {
                log.error("LLM 返回的题目 JSON 格式异常，原始内容: {}", questionsJson);
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "AI 生成的题目格式异常，请重试");
            }
            if (jsonArray.isEmpty()) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成的题目为空");
            }

            // 保存测验
            Quiz quiz = Quiz.builder()
                    .notebookId(notebookId)
                    .userId(loginUser.getId())
                    .title("测验 - " + QuizDifficultyEnum.getEnumByValue(generateRequest.getDifficulty()).getText())
                    .questionCount(jsonArray.size())
                    .difficulty(generateRequest.getDifficulty())
                    .questions(questionsJson)
                    .sourceIds(generateRequest.getSourceIds() != null ? generateRequest.getSourceIds().stream()
                            .map(String::valueOf).collect(Collectors.joining(",")) : null)
                    .build();
            quizService.save(quiz);

            return ResultUtils.success(quizService.getQuizVO(quiz));
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("测验生成失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "测验生成失败：" + e.getMessage());
        }
    }

    /**
     * 提交测验答案
     */
    @PostMapping("/submit")
    public BaseResponse<QuizRecord> submitQuiz(
            @RequestBody QuizSubmitRequest submitRequest,
            HttpServletRequest request) {

        ThrowUtils.throwIf(submitRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(submitRequest.getQuizId() == null, ErrorCode.PARAMS_ERROR);

        User loginUser = userService.getLoginUser(request);
        Quiz quiz = quizService.getById(submitRequest.getQuizId());
        ThrowUtils.throwIf(quiz == null, ErrorCode.NOT_FOUND_ERROR, "测验不存在");

        // 评分：解析题目答案并对比
        ThrowUtils.throwIf(quiz.getQuestions() == null || quiz.getQuestions().isBlank(),
                ErrorCode.SYSTEM_ERROR, "测验题目数据异常，请重新生成测验");
        JSONArray questions;
        try {
            questions = JSONUtil.parseArray(quiz.getQuestions());
        } catch (Exception e) {
            log.error("解析测验题目 JSON 失败, quizId={}", submitRequest.getQuizId(), e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "测验题目数据损坏，请重新生成测验");
        }
        int correctCount = 0;
        int totalCount = questions.size();

        for (QuizSubmitRequest.QuizAnswer answer : submitRequest.getAnswers()) {
            if (answer.getQuestionId() != null && answer.getQuestionId() <= totalCount) {
                var question = questions.getJSONObject(answer.getQuestionId() - 1);
                if (question != null) {
                    String correct = question.getStr("correctAnswer");
                    if (correct != null && correct.equalsIgnoreCase(answer.getAnswer())) {
                        correctCount++;
                    }
                }
            }
        }

        int score = totalCount > 0 ? (int) Math.round((double) correctCount / totalCount * 100) : 0;

        // 保存答题记录
        QuizRecord record = QuizRecord.builder()
                .quizId(submitRequest.getQuizId())
                .userId(loginUser.getId())
                .score(score)
                .correctCount(correctCount)
                .totalCount(totalCount)
                .timeCost(submitRequest.getTimeCost())
                .answers(JSONUtil.toJsonStr(submitRequest.getAnswers()))
                .build();
        quizRecordMapper.insert(record);

        return ResultUtils.success(record);
    }

    /** 测验列表 */
    @GetMapping("/list")
    public BaseResponse<List<QuizVO>> listQuizzes(
            @RequestParam Long notebookId,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        checkNotebookOwnership(notebookId, loginUser.getId());
        QueryWrapper qw = QueryWrapper.create()
                .eq("notebookId", notebookId)
                .orderBy("createTime", false);
        return ResultUtils.success(quizService.getQuizVOList(quizService.list(qw)));
    }

    /** 测验详情 */
    @GetMapping("/get")
    public BaseResponse<QuizVO> getQuiz(
            @RequestParam Long id,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Quiz quiz = quizService.getById(id);
        ThrowUtils.throwIf(quiz == null, ErrorCode.NOT_FOUND_ERROR);
        checkNotebookOwnership(quiz.getNotebookId(), loginUser.getId());
        return ResultUtils.success(quizService.getQuizVO(quiz));
    }

    /** 删除测验 */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteQuiz(
            @RequestBody DeleteRequest deleteRequest,
            HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Quiz quiz = quizService.getById(deleteRequest.getId());
        ThrowUtils.throwIf(quiz == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!quiz.getUserId().equals(loginUser.getId()), ErrorCode.NO_AUTH_ERROR);
        quizService.removeById(deleteRequest.getId());
        return ResultUtils.success(true);
    }

    private void checkNotebookOwnership(Long notebookId, Long userId) {
        Notebook notebook = notebookService.getById(notebookId);
        ThrowUtils.throwIf(notebook == null, ErrorCode.NOT_FOUND_ERROR);
        ThrowUtils.throwIf(!notebook.getUserId().equals(userId), ErrorCode.NO_AUTH_ERROR);
    }

    /**
     * 从 LLM 响应中提取 JSON 数组
     */
    private String extractJson(String text) {
        // 尝试找 ```json ... ``` 包裹
        int start = text.indexOf("```json");
        if (start != -1) {
            start = text.indexOf("\n", start) + 1;
            int end = text.indexOf("```", start);
            if (end != -1) {
                return text.substring(start, end).trim();
            }
        }
        // 尝试找 [ ... ]
        start = text.indexOf("[");
        int end = text.lastIndexOf("]");
        if (start != -1 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String buildQuizPrompt(int questionCount, String difficulty, String context) {
        String difficultyDesc = switch (difficulty) {
            case "easy" -> "简单（基础概念和事实性问题）";
            case "medium" -> "中等（需要理解和分析的问题）";
            case "hard" -> "困难（需要综合分析和深度理解的问题）";
            default -> "中等";
        };

        return """
                请根据以下文档内容，生成 %d 道选择题。

                要求：
                1. 难度级别：%s
                2. 每道题有且仅有 4 个选项（A/B/C/D）
                3. 必须有且仅有 1 个正确答案
                4. 提供答案解释
                5. 严格真实基于文档内容出题，不要编造不存在的信息

                请严格按照以下 JSON 数组格式输出，不要有其他多余内容：
                ```json
                [
                  {
                    "questionId": 1,
                    "question": "题目文本",
                    "options": [
                      {"label": "A", "text": "选项A的内容"},
                      {"label": "B", "text": "选项B的内容"},
                      {"label": "C", "text": "选项C的内容"},
                      {"label": "D", "text": "选项D的内容"}
                    ],
                    "correctAnswer": "A",
                    "explanation": "解析说明"
                  }
                ]
                ```

                文档内容：
                %s
                """.formatted(questionCount, difficultyDesc, context);
    }
}
