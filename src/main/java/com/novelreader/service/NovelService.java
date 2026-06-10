package com.novelreader.service;

import com.novelreader.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class NovelService {

    private static final Logger logger = LoggerFactory.getLogger(NovelService.class);

    @Autowired
    private DataService dataService;

    @Autowired
    private FileService fileService;

    /**
     * 导入小说
     */
    public ImportResult importNovel(MultipartFile file) {
        long startTime = System.currentTimeMillis();

        try {
            // 验证文件
            if (file == null || file.isEmpty()) {
                return new ImportResult(false, "文件为空");
            }

            String filename = file.getOriginalFilename();
            if (filename == null || !filename.toLowerCase().endsWith(".txt")) {
                return new ImportResult(false, "仅支持TXT格式文件");
            }

            logger.info("开始导入小说: {}", filename);

            // 读取文件内容
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) {
                return new ImportResult(false, "文件内容为空");
            }

            // 提取小说标题
            String novelName = extractNovelTitle(filename, content);
            logger.info("小说标题: {}", novelName);

            // 拆分章节
            List<FileService.ChapterContent> chapterContents =
                    fileService.splitIntoChapters(content, novelName);

            if (chapterContents.isEmpty()) {
                return new ImportResult(false, "未找到有效的章节内容");
            }

            // 创建小说对象
            Novel novel = new Novel();
            novel.setName(novelName);
            novel.setOriginalFilename(filename);
            novel.setTotalChapters(chapterContents.size());

            // 计算总字数
            long totalWords = chapterContents.stream()
                    .mapToLong(chapter -> chapter.getContent().length())
                    .sum();
            novel.setTotalWords(totalWords);

            // 保存原始文件
            String filePath = fileService.saveUploadedFile(
                    novel.getId(), filename, file.getBytes()
            );
            novel.setStoragePath(filePath);

            // 保存章节并创建Chapter对象
            List<Chapter> chapters = new ArrayList<>();
            for (FileService.ChapterContent chapterContent : chapterContents) {
                String chapterFilePath = fileService.saveChapterFile(
                        novel.getId(),
                        chapterContent.getChapterNumber(),
                        chapterContent.getTitle(),
                        chapterContent.getContent()
                );

                Chapter chapter = new Chapter(
                        chapterContent.getChapterNumber(),
                        chapterContent.getTitle(),
                        chapterContent.getContent(),
                        chapterFilePath
                );
                chapters.add(chapter);
            }
            novel.setChapter(chapters);

            // 保存小说数据
            dataService.addNovel(novel);

            long processingTime = System.currentTimeMillis() - startTime;

            ImportResult result = new ImportResult(true, "导入成功");
            result.setNovelId(novel.getId());
            result.setNovelTitle(novelName);
            result.setTotalChapters(chapterContents.size());
            result.setProcessingTime(processingTime);

            logger.info("小说导入完成: {} ({} 章, {}ms)",
                    novelName, chapterContents.size(), processingTime);

            return result;

        } catch (IOException e) {
            logger.error("导入小说失败", e);
            return new ImportResult(false, "文件处理失败: " + e.getMessage());
        } catch (Exception e) {
            logger.error("导入小说时发生未知错误", e);
            return new ImportResult(false, "导入过程中发生错误: " + e.getMessage());
        }
    }

    /**
     * 提取小说标题
     */
    private String extractNovelTitle(String filename, String content) {
        String Name = null;

        // 尝试从内容中提取标题
        if (content.startsWith("书名：") || content.startsWith("书名:")) {
            int start = content.indexOf("：") + 1;
            if (start == 0) start = content.indexOf(":") + 1;
            if (start > 0) {
                int end = content.indexOf('\n', start);
                if (end > start) {
                    Name = content.substring(start, end).trim();
                }
            }
        }

        // 如果没找到，使用文件名
        if (Name == null || Name.isEmpty()) {
            Name = filename.replace(".txt", "").replace(".TXT", "");
        }

        // 清理标题
        return Name.replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 获取所有小说
     */
    public List<Novel> getAllNovels() {
        return dataService.getAllNovels();
    }

    /**
     * 获取小说详情
     */
    public Novel getNovelById(String novelId) {
        return dataService.getNovelById(novelId);
    }

    /**
     * 获取章节列表
     */
    public List<Chapter> getChapters(String novelId) {
        Novel novel = dataService.getNovelById(novelId);
        if (novel == null) {
            throw new IllegalArgumentException("小说不存在");
        }

        return novel.getChapters() != null ? novel.getChapter() : new ArrayList<>();
    }

    /**
     * 获取章节内容
     */
    public String getChapterContent(String novelId, int chapterNumber) throws IOException {
        Novel novel = dataService.getNovelById(novelId);
        if (novel == null) {
            throw new IllegalArgumentException("小说不存在");
        }

        // 查找章节
        if (novel.getChapters() != null) {
            for (Chapter chapter : novel.getChapter()) {
                if (chapter.getChapterNumber() == chapterNumber) {
                    if (chapter.getContent() != null) {
                        return chapter.getContent();
                    }
                    // 如果内容为空，从文件读取
                    if (chapter.getFilePath() != null) {
                        return fileService.readFileContent(chapter.getFilePath());
                    }
                }
            }
        }

        throw new IllegalArgumentException("章节不存在: " + chapterNumber);
    }

    /**
     * 更新阅读进度
     */
    public void updateReadingProgress(String novelId, int chapterNumber, String chapterTitle) throws IOException {
        Novel novel = dataService.getNovelById(novelId);
        if (novel == null) {
            throw new IllegalArgumentException("小说不存在");
        }

        // 更新小说信息
        novel.setLastReadAt(LocalDateTime.now());
        novel.setLastReadChapter(chapterNumber);
        novel.setLastReadChapterTitle(chapterTitle);
        dataService.updateNovel(novel);

        // 保存阅读进度
        ReadingProgress progress = new ReadingProgress(
                novelId,
                novel.getName(),
                chapterNumber,
                chapterTitle
        );
        dataService.addReadingProgress(progress);

        logger.info("更新阅读进度: {} - 第{}章 {}",
                novel.getName(), chapterNumber, chapterTitle);
    }

    /**
     * 删除小说
     */
    public boolean deleteNovel(String novelId) throws IOException {
        // 删除文件
        fileService.deleteNovelFiles(novelId);

        // 删除数据
        return dataService.deleteNovel(novelId);
    }

    /**
     * 获取阅读历史
     */
    public List<ReadingProgress> getReadingHistory() {
        return dataService.getReadingHistory();
    }

    /**
     * 搜索小说
     */
    public List<Novel> searchNovels(String keyword) {
        return dataService.searchNovels(keyword);
    }
}