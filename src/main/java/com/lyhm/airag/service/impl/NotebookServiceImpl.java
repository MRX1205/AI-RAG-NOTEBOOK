package com.lyhm.airag.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import com.lyhm.airag.config.VectorStoreConfig;
import com.lyhm.airag.mapper.NotebookMapper;
import com.lyhm.airag.mapper.QuizMapper;
import com.lyhm.airag.mapper.QuizRecordMapper;
import com.lyhm.airag.mapper.ReportMapper;
import com.lyhm.airag.mapper.SourceMapper;
import com.lyhm.airag.mapper.UserMapper;
import com.lyhm.airag.model.entity.Notebook;
import com.lyhm.airag.model.entity.Quiz;
import com.lyhm.airag.model.entity.User;
import com.lyhm.airag.model.vo.FeaturedNotebookVO;
import com.lyhm.airag.model.vo.NotebookVO;
import com.lyhm.airag.service.NotebookService;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.lyhm.airag.exception.BusinessException;
import com.lyhm.airag.exception.ErrorCode;

/**
 * 笔记本服务实现类
 */
@Slf4j
@Service
public class NotebookServiceImpl extends ServiceImpl<NotebookMapper, Notebook>
                implements NotebookService {

        @Resource
        private SourceMapper sourceMapper;

        @Resource
        private ReportMapper reportMapper;

        @Resource
        private QuizMapper quizMapper;

        @Resource
        private QuizRecordMapper quizRecordMapper;

        @Resource
        private VectorStoreConfig vectorStoreConfig;

        @Resource
        private UserMapper userMapper;

        @Override
        public NotebookVO getNotebookVO(Notebook notebook) {
                if (notebook == null) {
                        return null;
                }
                NotebookVO notebookVO = new NotebookVO();
                BeanUtil.copyProperties(notebook, notebookVO);
                return notebookVO;
        }

        @Override
        public List<NotebookVO> getNotebookVOList(List<Notebook> notebookList) {
                return notebookList.stream()
                                .map(this::getNotebookVO)
                                .collect(Collectors.toList());
        }

        /**
         * 级联删除笔记本及所有关联数据
         */
        @Override
        @Transactional(rollbackFor = Exception.class)
        public void deleteNotebookCascade(Long notebookId) {
                log.info("开始级联删除笔记本: {}", notebookId);

                List<Quiz> quizList = quizMapper.selectListByQuery(
                                QueryWrapper.create().eq("notebookId", notebookId));
                for (Quiz quiz : quizList) {
                        quizRecordMapper.deleteByQuery(
                                        QueryWrapper.create().eq("quizId", quiz.getId()));
                }

                quizMapper.deleteByQuery(QueryWrapper.create().eq("notebookId", notebookId));
                reportMapper.deleteByQuery(QueryWrapper.create().eq("notebookId", notebookId));
                sourceMapper.deleteByQuery(QueryWrapper.create().eq("notebookId", notebookId));
                vectorStoreConfig.removeStore(notebookId);
                this.removeById(notebookId);

                log.info("笔记本 {} 级联删除完成", notebookId);
        }

        /**
         * 获取精选笔记本列表（最多12条，含用户名）
         */
        @Override
        public List<FeaturedNotebookVO> getFeaturedNotebooks() {
                QueryWrapper queryWrapper = QueryWrapper.create()
                                .eq("isFeatured", 1)
                                .orderBy("createTime", false)
                                .limit(12);
                List<Notebook> notebooks = this.list(queryWrapper);

                List<FeaturedNotebookVO> result = new ArrayList<>();
                for (Notebook notebook : notebooks) {
                        FeaturedNotebookVO vo = new FeaturedNotebookVO();
                        BeanUtil.copyProperties(notebook, vo);
                        // 查询所属用户名
                        User user = userMapper.selectOneById(notebook.getUserId());
                        if (user != null) {
                                vo.setUserName(user.getUserName());
                        }
                        result.add(vo);
                }
                return result;
        }

        /**
         * 管理员分页查询所有笔记本
         */
        @Override
        public Page<NotebookVO> getAdminNotebookPage(long pageNum, long pageSize, String title, String userName) {
                // 先按条件查用户ID（如果传了userName）
                QueryWrapper notebookQuery = QueryWrapper.create();
                if (title != null && !title.isEmpty()) {
                        notebookQuery.like("title", title);
                }
                if (userName != null && !userName.isEmpty()) {
                        // 查符合用户名的用户ID列表
                        QueryWrapper userQuery = QueryWrapper.create().like("userName", userName);
                        List<User> users = userMapper.selectListByQuery(userQuery);
                        if (users.isEmpty()) {
                                Page<NotebookVO> empty = new Page<>(pageNum, pageSize, 0);
                                empty.setRecords(new ArrayList<>());
                                return empty;
                        }
                        List<Long> userIds = users.stream().map(User::getId).collect(Collectors.toList());
                        notebookQuery.in("userId", userIds);
                }
                notebookQuery.orderBy("createTime", false);

                Page<Notebook> notebookPage = this.page(Page.of(pageNum, pageSize), notebookQuery);
                Page<NotebookVO> voPage = new Page<>(pageNum, pageSize, notebookPage.getTotalRow());

                List<NotebookVO> voList = new ArrayList<>();
                for (Notebook notebook : notebookPage.getRecords()) {
                        NotebookVO vo = getNotebookVO(notebook);
                        // 填充用户名
                        User user = userMapper.selectOneById(notebook.getUserId());
                        if (user != null) {
                                vo.setUserName(user.getUserName());
                        }
                        voList.add(vo);
                }
                voPage.setRecords(voList);
                return voPage;
        }

        /**
         * 上传笔记本封面图片
         */
        @Override
        public String uploadCover(Long notebookId, MultipartFile file, String uploadDir) {
                if (file == null || file.isEmpty()) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
                }
                String originalFilename = file.getOriginalFilename();
                if (originalFilename == null) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不能为空");
                }
                String extension = FileUtil.extName(originalFilename).toLowerCase();
                if (!List.of("jpg", "jpeg", "png", "gif", "webp").contains(extension)) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "封面只支持 jpg/png/gif/webp 格式");
                }
                if (file.getSize() > 5 * 1024 * 1024) {
                        throw new BusinessException(ErrorCode.PARAMS_ERROR, "封面文件不能超过5MB");
                }

                String savedFileName = UUID.randomUUID() + "." + extension;
                Path savePath = Paths.get(uploadDir, "covers", savedFileName);
                try {
                        Files.createDirectories(savePath.getParent());
                        file.transferTo(savePath.toFile());
                } catch (IOException e) {
                        log.error("封面保存失败", e);
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "封面保存失败");
                }

                String coverUrl = "/uploads/covers/" + savedFileName;
                // 更新笔记本封面
                Notebook notebook = new Notebook();
                notebook.setId(notebookId);
                notebook.setCoverImage(coverUrl);
                this.updateById(notebook);
                return coverUrl;
        }
}
