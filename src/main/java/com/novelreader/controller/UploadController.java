package com.novelreader.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.novelreader.utils.ChapterSplitter;
import com.novelreader.model.ChapterInfo;  // 添加这行导入

import java.io.*;
import java.nio.file.*;
import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.nio.charset.StandardCharsets;  // 如果需要指定编码

@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final Logger logger = LoggerFactory.getLogger(UploadController.class);
    // 上传文件保存的目录
    //private static final String UPLOAD_DIR = "F:\\soi\\novel-reader\\novel-storage\\";
    @Value("${app.novel-storage-path}")
    private String novelStoragePath;
    // novels.json 文件路径
    //private static final String NOVELS_JSON_PATH = "F:\\soi\\novel-reader\\novel-data\\novels.json";
    @Value("${app.novel-data-path}")
    private String novelDataPath;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 上传并处理小说文件
     */
    @PostMapping("/simple")
    public Map<String, Object> uploadSimple(@RequestParam("file") MultipartFile file) {
        Map<String, Object> result = new HashMap<>();

        try {
            logger.info("接收到上传文件: {}", file.getOriginalFilename());

            if (file.isEmpty()) {
                result.put("success", false);
                result.put("message", "文件为空");
                return result;
            }

            // 使用原始文件名（去掉扩展名）
            String originalFilename = file.getOriginalFilename();
            if (originalFilename == null || originalFilename.trim().isEmpty()) {
                originalFilename = "unknown_" + System.currentTimeMillis();
            }

            // 提取书名（去掉.txt扩展名）
            String novelName = originalFilename.replace(".txt", "");
            if (novelName.endsWith(".TXT") || novelName.endsWith(".Txt")) {
                novelName = novelName.substring(0, novelName.length() - 4);
            }

            // 清理文件名中的非法字符
            String safeFilename = originalFilename
                    .replaceAll("[\\\\/:*?\"<>|]", "_")
                    .replaceAll("\\s+", "_");

            // 生成唯一的书籍ID
            String novelId = "novel_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);

            // 创建书籍目录
            String novelDirPath = novelStoragePath + novelId + "\\";
            Files.createDirectories(Paths.get(novelDirPath));

            // 保存原始文件
            String originalFilePath = novelDirPath + "original.txt";
            Path originalPath = Paths.get(originalFilePath);
            Files.copy(file.getInputStream(), originalPath);

            // 读取文件内容
            String content = new String(Files.readAllBytes(originalPath), StandardCharsets.UTF_8);

            // 分割章节
            List<ChapterInfo> chapters = ChapterSplitter.splitChapters(content);

            // 保存章节文件
            List<String> chapterTitles = new ArrayList<>();
            for (ChapterInfo chapter : chapters) {
                // 保存章节内容到单独的txt文件
                String chapterFileName = chapter.getIndex() + ".txt";
                String chapterFilePath = novelDirPath + chapterFileName;

                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(new FileOutputStream(chapterFilePath), StandardCharsets.UTF_8))) {
                    writer.write(chapter.getContent());
                }

                // 添加章节标题到列表
                chapterTitles.add(chapter.getTitle());

                logger.debug("保存章节 {}: {}, 字数: {}",
                        chapter.getIndex(), chapter.getTitle(), chapter.getWordCount());
            }

            logger.info("小说分割完成，共 {} 章", chapters.size());

            // 创建小说信息
            Map<String, Object> novelInfo = new HashMap<>();
            novelInfo.put("id", novelId);
            novelInfo.put("name", novelName);
            novelInfo.put("chapter", chapterTitles);  // 章节标题列表
            novelInfo.put("history", 0);  // 初始阅读进度为0

            // 更新 novels.json
            updateNovelsJson(novelInfo);

            // 验证文件
            File savedFile = originalPath.toFile();
            long fileSize = savedFile.length();

            result.put("success", true);
            result.put("message", "文件上传和处理成功");
            result.put("filename", safeFilename);
            result.put("originalFilename", originalFilename);
            result.put("novelName", novelName);
            result.put("novelId", novelId);
            result.put("totalChapters", chapters.size());
            result.put("path", originalFilePath);
            result.put("size", fileSize);
            result.put("timestamp", System.currentTimeMillis());

            logger.info("小说上传处理成功: {}, ID: {}, 章节数: {}", novelName, novelId, chapters.size());

        } catch (Exception e) {
            logger.error("文件上传处理失败", e);
            result.put("success", false);
            result.put("message", "上传处理失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 更新 novels.json 文件
     */
    private synchronized void updateNovelsJson(Map<String, Object> novelInfo) throws IOException {
        File jsonFile = new File(novelDataPath);

        // 确保目录存在
        jsonFile.getParentFile().mkdirs();

        ArrayNode novelsArray = objectMapper.createArrayNode();

        // 检查文件是否存在且不为空
        if (jsonFile.exists() && jsonFile.length() > 0) {
            try {
                // 读取现有的JSON数据
                JsonNode rootNode = objectMapper.readTree(jsonFile);

                if (rootNode.isArray()) {
                    novelsArray = (ArrayNode) rootNode;
                    logger.info("成功读取现有的 novels.json，已有 {} 本小说", novelsArray.size());
                } else {
                    logger.warn("novels.json 格式不正确，不是数组格式，将创建新的数组");
                    novelsArray = objectMapper.createArrayNode();
                }
            } catch (Exception e) {
                logger.warn("读取 novels.json 失败，将创建新的文件。错误: {}", e.getMessage());
                // 备份旧文件
                try {
                    File backupFile = new File(novelDataPath + ".backup_" + System.currentTimeMillis());
                    Files.copy(jsonFile.toPath(), backupFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    logger.info("已备份旧文件到: {}", backupFile.getAbsolutePath());
                } catch (Exception backupEx) {
                    logger.warn("备份旧文件失败: {}", backupEx.getMessage());
                }
                novelsArray = objectMapper.createArrayNode();
            }
        } else {
            logger.info("novels.json 文件不存在或为空，将创建新文件");
        }

        // 检查是否已存在相同ID的小说
        boolean novelExists = false;
        for (JsonNode node : novelsArray) {
            if (node.has("id") && node.get("id").asText().equals(novelInfo.get("id"))) {
                novelExists = true;
                break;
            }
        }

        if (!novelExists) {
            // 添加新的小说信息
            ObjectNode novelNode = objectMapper.createObjectNode();
            novelNode.put("id", (String) novelInfo.get("id"));
            novelNode.put("name", (String) novelInfo.get("name"));
            novelNode.put("history", 0);  // 初始阅读进度为0

            // 添加章节列表
            ArrayNode chapterArray = objectMapper.createArrayNode();
            List<String> chapters = (List<String>) novelInfo.get("chapter");
            if (chapters != null) {
                for (String chapterTitle : chapters) {
                    chapterArray.add(chapterTitle);
                }
            }
            novelNode.set("chapter", chapterArray);

            novelsArray.add(novelNode);

            // 写回文件（使用UTF-8编码）
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(jsonFile, novelsArray);

            logger.info("已更新 novels.json，新增小说: {}，当前共 {} 本小说",
                    novelInfo.get("name"), novelsArray.size());
        } else {
            logger.info("小说已存在，无需重复添加: {}", novelInfo.get("name"));
        }
    }

    /**
     * 测试连接
     */
    @GetMapping("/test")
    public Map<String, Object> test() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "ok");
        result.put("message", "上传API正常工作");
        result.put("timestamp", System.currentTimeMillis());
        result.put("uploadDir", novelStoragePath);
        result.put("novelsJsonPath", novelDataPath);

        // 检查novels.json是否存在
        File jsonFile = new File(novelDataPath);
        result.put("novelsJsonExists", jsonFile.exists());

        return result;
    }
}