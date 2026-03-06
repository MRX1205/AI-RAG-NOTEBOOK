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
import com.lyhm.airag.model.entity.Source;
import com.lyhm.airag.model.entity.User;
import com.lyhm.airag.model.vo.FeaturedNotebookVO;
import com.lyhm.airag.model.vo.NotebookVO;
import com.lyhm.airag.model.vo.PublicNotebookDetailVO;
import com.lyhm.airag.model.vo.SourceVO;
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
                        // 填充 sourceCount（notebook 实体已有冗余字段）
                        if (notebook.getSourceCount() != null) {
                                vo.setSourceCount(notebook.getSourceCount());
                        }
                        result.add(vo);
                }
                return result;
        }

        /**
         * 获取单个精选笔记本的公开详情（含来源列表）
         * 仅 isFeatured=1 的笔记本可通过此接口访问
         */
        @Override
        public PublicNotebookDetailVO getPublicNotebookDetail(Long notebookId) {
                Notebook notebook = this.getById(notebookId);
                if (notebook == null) {
                        throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "笔记本不存在");
                }
                if (notebook.getIsFeatured() == null || notebook.getIsFeatured() != 1) {
                        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "该笔记本不是精选笔记本，无法公开访问");
                }

                PublicNotebookDetailVO vo = new PublicNotebookDetailVO();
                BeanUtil.copyProperties(notebook, vo);

                // 查询所属用户名
                User user = userMapper.selectOneById(notebook.getUserId());
                if (user != null) {
                        vo.setUserName(user.getUserName());
                }

                // 查询来源列表（只返回元数据，不返回文件内容）
                List<Source> sources = sourceMapper.selectListByQuery(
                                QueryWrapper.create().eq("notebookId", notebookId).orderBy("createTime", true));
                List<SourceVO> sourceVOs = sources.stream().map(s -> {
                        SourceVO svo = new SourceVO();
                        BeanUtil.copyProperties(s, svo);
                        return svo;
                }).collect(Collectors.toList());
                vo.setSources(sourceVOs);

                return vo;
        }

        /**
         * 克隆笔记本：为指定用户创建一个副本笔记本
         * 1. 复制笔记本元数据（title, description, summary, coverImage），isFeatured 重置为 0
         * 2. 复制所有来源记录（source 记录，共享原文件路径）
         * 3. 复制向量存储文件，使 RAG 检索立即可用
         */
        @Override
        @Transactional(rollbackFor = Exception.class)
        public Long cloneNotebook(Long notebookId, Long userId) {
                // 获取源笔记本
                Notebook source = this.getById(notebookId);
                if (source == null) {
                        throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "源笔记本不存在");
                }
                if (source.getIsFeatured() == null || source.getIsFeatured() != 1) {
                        throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "只能克隆精选笔记本");
                }

                // 创建新笔记本（副本标题前加「副本」标注）
                Notebook newNotebook = new Notebook();
                newNotebook.setTitle("「副本」" + source.getTitle());
                newNotebook.setDescription(source.getDescription());
                newNotebook.setSummary(source.getSummary());
                newNotebook.setCoverImage(source.getCoverImage());
                newNotebook.setUserId(userId);
                newNotebook.setIsFeatured(0); // 副本不是精选
                newNotebook.setSourceCount(0); // 先设为 0，后续更新
                boolean saved = this.save(newNotebook);
                if (!saved) {
                        throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建副本笔记本失败");
                }

                Long newNotebookId = newNotebook.getId();

                // 复制来源记录
                List<Source> sourceList = sourceMapper.selectListByQuery(
                                QueryWrapper.create().eq("notebookId", notebookId).orderBy("createTime", true));
                int copiedCount = 0;
                java.time.LocalDateTime now = java.time.LocalDateTime.now();
                for (Source src : sourceList) {
                        Source newSource = new Source();
                        BeanUtil.copyProperties(src, newSource);
                        newSource.setId(null); // 清空 ID，让 MyBatis-Flex 自动生成新 ID
                        newSource.setNotebookId(newNotebookId);
                        newSource.setUserId(userId);
                        // 使用当前时间而非 null，避免 NOT NULL 约束错误
                        newSource.setCreateTime(now);
                        newSource.setUpdateTime(now);
                        newSource.setIsDelete(0); // 确保副本状态为未删除
                        sourceMapper.insert(newSource);
                        copiedCount++;
                }

                // 更新来源数量
                Notebook updateCount = new Notebook();
                updateCount.setId(newNotebookId);
                updateCount.setSourceCount(copiedCount);
                this.updateById(updateCount);

                // 复制向量存储（使副本可直接使用 RAG 检索）
                vectorStoreConfig.copyStore(notebookId, newNotebookId);

                log.info("笔记本 {} 已克隆为 {}，来源数量: {}", notebookId, newNotebookId, copiedCount);
                return newNotebookId;
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
                // toAbsolutePath()：避免 transferTo() 将相对路径解析到 Tomcat 工作目录
                Path savePath = Paths.get(uploadDir, "covers", savedFileName).toAbsolutePath().normalize();
                try {
                        Files.createDirectories(savePath.getParent());
                        // 用 Files.copy + InputStream 代替 transferTo，彻底规避 Tomcat 路径问题
                        try (java.io.InputStream inputStream = file.getInputStream()) {
                                Files.copy(inputStream, savePath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                        }
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
