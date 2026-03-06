package com.lyhm.airag.service;

import com.lyhm.airag.model.entity.Quiz;
import com.lyhm.airag.model.vo.QuizVO;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * 测验服务接口
 */
public interface QuizService extends IService<Quiz> {

    QuizVO getQuizVO(Quiz quiz);

    List<QuizVO> getQuizVOList(List<Quiz> quizList);
}
