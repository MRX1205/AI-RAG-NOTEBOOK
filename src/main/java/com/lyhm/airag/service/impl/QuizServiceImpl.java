package com.lyhm.airag.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.lyhm.airag.mapper.QuizMapper;
import com.lyhm.airag.model.entity.Quiz;
import com.lyhm.airag.model.vo.QuizVO;
import com.lyhm.airag.service.QuizService;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 测验服务实现类
 */
@Service
public class QuizServiceImpl extends ServiceImpl<QuizMapper, Quiz>
        implements QuizService {

    @Override
    public QuizVO getQuizVO(Quiz quiz) {
        if (quiz == null)
            return null;
        QuizVO vo = new QuizVO();
        BeanUtil.copyProperties(quiz, vo);
        return vo;
    }

    @Override
    public List<QuizVO> getQuizVOList(List<Quiz> quizList) {
        return quizList.stream().map(this::getQuizVO).collect(Collectors.toList());
    }
}
