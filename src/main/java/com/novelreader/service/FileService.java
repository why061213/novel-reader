package com.novelreader.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class FileService {

    private static final Logger logger = LoggerFactory.getLogger(FileService.class);

    @Value("${novel.storage.base-path:./novel-storage}")
    private String storageBasePath;

    // 章节标题正则表达式
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
            "^\\s*(第[零一二三四五六七八九十百千万\\d]+章|[上下卷]|第\\d+节|[一二三四五六七八九十]\\s*[、.])\\s*.*$",
            Pattern.MULTILINE
    );

    /**
     * 保存上传的文件
     */
    public String saveUploadedFile(String novelId, String filename, byte[] content) throws IOException {
        Path novelDir = getNovelDirectory(novelId);
        Files.createDirectories(novelDir);

        Path filePath = novelDir.resolve(sanitizeFilename(filename));
        Files.write(filePath, content);

        logger.info("文件已保存: {}", filePath);
        return filePath.toString();
    }

    /**
     * 读取文件内容
     */
    public String readFileContent(String filePath) throws IOException {
        return Files.readString(Paths.get(filePath), StandardCharsets.UTF_8);
    }

    /**
     * 拆分章节
     */
    public List<ChapterContent> splitIntoChapters(String content, String novelTitle) {
        List<ChapterContent> chapters = new ArrayList<>();

        if (content == null || content.trim().isEmpty()) {
            return chapters;
        }

        // 规范化内容
        content = normalizeContent(content);

        // 查找章节起始位置
        Matcher matcher = CHAPTER_PATTERN.matcher(content);
        List<ChapterMatch> matches = new ArrayList<>();

        while (matcher.find()) {
            matches.add(new ChapterMatch(matcher.start(), matcher.group().trim()));
        }

        if (matches.isEmpty()) {
            // 没有找到章节标题，按固定长度拆分
            chapters = splitByLength(content, novelTitle);
        } else {
            // 按章节标题拆分
            chapters = splitByChapterMatches(content, matches);
        }

        // 如果还是没有章节，整个作为一章
        if (chapters.isEmpty()) {
            chapters.add(new ChapterContent(1, novelTitle, content));
        }

        return chapters;
    }

    /**
     * 按章节匹配拆分
     */
    private List<ChapterContent> splitByChapterMatches(String content, List<ChapterMatch> matches) {
        List<ChapterContent> chapters = new ArrayList<>();
        int lastEnd = 0;
        int chapterNumber = 1;

        for (int i = 0; i < matches.size(); i++) {
            ChapterMatch match = matches.get(i);

            // 确定本章结束位置
            int endPos = (i + 1 < matches.size()) ? matches.get(i + 1).position : content.length();

            // 提取本章内容
            String chapterContent = content.substring(match.position, endPos).trim();

            if (!chapterContent.isEmpty() && chapterContent.length() > 100) {
                // 提取标题（第一行）
                String title = extractChapterTitle(chapterContent);
                if (title == null || title.trim().isEmpty()) {
                    title = "第" + chapterNumber + "章";
                }

                chapters.add(new ChapterContent(chapterNumber, title, chapterContent));
                chapterNumber++;
            }

            lastEnd = endPos;
        }

        return chapters;
    }

    /**
     * 按长度拆分
     */
    private List<ChapterContent> splitByLength(String content, String novelTitle) {
        List<ChapterContent> chapters = new ArrayList<>();

        int chapterLength = 3000; // 每章大约3000字
        int totalLength = content.length();
        int start = 0;
        int chapterNumber = 1;

        while (start < totalLength) {
            int end = Math.min(start + chapterLength, totalLength);

            // 尝试在段落结束处拆分
            if (end < totalLength) {
                int newlineIndex = content.indexOf("\n\n", end);
                if (newlineIndex != -1 && newlineIndex - start < chapterLength * 1.5) {
                    end = newlineIndex + 2;
                }
            }

            String chapterContent = content.substring(start, end).trim();
            if (!chapterContent.isEmpty() && chapterContent.length() > 100) {
                chapters.add(new ChapterContent(
                        chapterNumber,
                        "第" + chapterNumber + "章",
                        chapterContent
                ));
                chapterNumber++;
            }

            start = end;
        }

        return chapters;
    }

    /**
     * 保存章节文件
     */
    public String saveChapterFile(String novelId, int chapterNumber, String title, String content) throws IOException {
        Path chapterDir = getChapterDirectory(novelId);
        Files.createDirectories(chapterDir);

        // 生成安全的文件名
        String safeTitle = sanitizeFilename(title);
        if (safeTitle.length() > 50) {
            safeTitle = safeTitle.substring(0, 50);
        }

        String filename = String.format("chapter_%03d_%s.txt", chapterNumber, safeTitle);
        Path filePath = chapterDir.resolve(filename);

        Files.writeString(filePath, content, StandardCharsets.UTF_8);

        return filePath.toString();
    }

    /**
     * 删除小说相关文件
     */
    public void deleteNovelFiles(String novelId) throws IOException {
        Path novelDir = getNovelDirectory(novelId);
        Path chapterDir = getChapterDirectory(novelId);

        if (Files.exists(novelDir)) {
            deleteDirectory(novelDir);
        }

        if (Files.exists(chapterDir)) {
            deleteDirectory(chapterDir);
        }
    }

    /**
     * 提取章节标题
     */
    private String extractChapterTitle(String content) {
        if (content == null || content.isEmpty()) {
            return null;
        }

        int newlineIndex = content.indexOf('\n');
        if (newlineIndex != -1) {
            return content.substring(0, newlineIndex).trim();
        }

        return content.trim();
    }

    /**
     * 规范化内容
     */
    private String normalizeContent(String content) {
        if (content == null) {
            return "";
        }

        // 统一换行符
        content = content.replaceAll("\\r\\n", "\n")
                .replaceAll("\\r", "\n");

        // 压缩多余的空白行
        content = content.replaceAll("\\n{3,}", "\n\n");

        return content.trim();
    }

    /**
     * 清理文件名
     */
    private String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unnamed";
        }

        return filename.replaceAll("[\\\\/:*?\"<>|]", "_")
                .replaceAll("\\s+", "_")
                .trim();
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(Path path) throws IOException {
        if (Files.isDirectory(path)) {
            try (DirectoryStream<Path> entries = Files.newDirectoryStream(path)) {
                for (Path entry : entries) {
                    deleteDirectory(entry);
                }
            }
        }
        Files.deleteIfExists(path);
    }

    /**
     * 获取小说目录
     */
    private Path getNovelDirectory(String novelId) {
        return Paths.get(storageBasePath, "novels", novelId);
    }

    /**
     * 获取章节目录
     */
    private Path getChapterDirectory(String novelId) {
        return Paths.get(storageBasePath, "chapters", novelId);
    }

    // 内部类
    private static class ChapterMatch {
        final int position;
        final String title;

        ChapterMatch(int position, String title) {
            this.position = position;
            this.title = title;
        }
    }

    // 章节内容包装类
    public static class ChapterContent {
        private final int chapterNumber;
        private final String title;
        private final String content;

        public ChapterContent(int chapterNumber, String title, String content) {
            this.chapterNumber = chapterNumber;
            this.title = title;
            this.content = content;
        }

        public int getChapterNumber() { return chapterNumber; }
        public String getTitle() { return title; }
        public String getContent() { return content; }
    }
}