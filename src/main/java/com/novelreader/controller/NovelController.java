package com.novelreader.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@RestController
@RequestMapping("/api/novels")
public class NovelController {

    private static final Logger logger = LoggerFactory.getLogger(NovelController.class);

    @Value("${app.novel-data-path}")
    private String novelDataPath;

    @Value("${app.novel-storage-path}")
    private String novelStoragePath;
    //private static final String NOVELS_JSON_PATH =
    //        System.getProperty("user.dir") + File.separator + "novel-data" + File.separator + "novels.json";

    //private static final String NOVELS_STORAGE_PATH =
    //        System.getProperty("user.dir") + File.separator + "novel-storage" + File.separator;
    // novels.json 文件路径
    //private static final String NOVELS_JSON_PATH = "novel-data/novels.json";

    // 小说存储根目录
    //private static final String NOVELS_STORAGE_PATH = "novel-storage";

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取所有小说
     */
    @GetMapping
    public List<Map<String, Object>> getAllNovels() {
        List<Map<String, Object>> novels = new ArrayList<>();

        try {

            File jsonFile = new File(novelDataPath);

            if (!jsonFile.exists()) {
                logger.info("novels.json 文件不存在");
                return novels;
            }

            // 读取JSON文件
            String jsonContent = new String(Files.readAllBytes(jsonFile.toPath()), "UTF-8");

            if (jsonContent.trim().isEmpty()) {
                return novels;
            }

            // 解析JSON
            JsonNode rootNode = objectMapper.readTree(jsonContent);

            if (rootNode.isArray()) {
                for (JsonNode novelNode : rootNode) {
                    Map<String, Object> novel = new HashMap<>();

                    // 基本字段
                    if (novelNode.has("id")) {
                        novel.put("id", novelNode.get("id").asText());
                    }
                    if (novelNode.has("name")) {
                        novel.put("name", novelNode.get("name").asText());
                    }
                    if (novelNode.has("history")) {
                        novel.put("history", novelNode.get("history").asInt());
                    }

                    // 章节列表
                    if (novelNode.has("chapter")) {
                        List<String> chapters = new ArrayList<>();
                        JsonNode chapterArray = novelNode.get("chapter");
                        if (chapterArray.isArray()) {
                            for (JsonNode chapter : chapterArray) {
                                chapters.add(chapter.asText());
                            }
                        }
                        novel.put("chapter", chapters);
                    }

                    // 添加额外信息
                    novel.put("totalChapters", novel.containsKey("chapter") ?
                            ((List<?>) novel.get("chapter")).size() : 0);

                    novels.add(novel);

                    if (novelNode.has("history")) {
                        novel.put("history", novelNode.get("history").asInt());
                    }
                    // 新增 lastRead 读取
                    if (novelNode.has("lastRead")) {
                        novel.put("lastRead", novelNode.get("lastRead").asInt());
                    } else {
                        // 兼容旧数据，默认等于 history
                        novel.put("lastRead", novelNode.has("history") ? novelNode.get("history").asInt() : 0);
                    }
                }

            }

            logger.info("获取到 {} 本小说", novels.size());

        } catch (Exception e) {
            logger.error("获取小说列表失败", e);
        }

        return novels;
    }

    /**
     * 获取小说详情
     */
    @GetMapping("/{novelId}")
    public Map<String, Object> getNovel(@PathVariable String novelId) {
        Map<String, Object> result = new HashMap<>();

        try {
            // 从 novels.json 中查找指定的小说
            List<Map<String, Object>> allNovels = getAllNovels();

            for (Map<String, Object> novel : allNovels) {
                if (novel.get("id").equals(novelId)) {
                    // 获取章节内容统计
                    String novelDirPath = novelStoragePath + novelId + "\\";
                    File novelDir = new File(novelDirPath);

                    if (novelDir.exists()) {
                        // 统计章节文件
                        int totalChapters = (int) novel.get("totalChapters");
                        int actualChapters = 0;

                        if (novelDir.listFiles() != null) {
                            for (File file : novelDir.listFiles()) {
                                if (file.getName().matches("\\d+\\.txt")) {
                                    actualChapters++;
                                }
                            }
                        }

                        novel.put("actualChapters", actualChapters);
                        novel.put("storagePath", novelDirPath);
                    }

                    return novel;
                }
            }

            result.put("success", false);
            result.put("message", "小说不存在");

        } catch (Exception e) {
            logger.error("获取小说详情失败", e);
            result.put("success", false);
            result.put("message", "获取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取章节列表
     */
    @GetMapping("/{novelId}/chapters")
    public List<Map<String, Object>> getChapters(@PathVariable String novelId) {
        List<Map<String, Object>> chapters = new ArrayList<>();

        try {
            // 获取小说信息
            Map<String, Object> novel = getNovel(novelId);

            if (!novel.containsKey("chapter") || novel.get("success") != null) {
                return chapters;
            }

            // 获取章节列表
            List<String> chapterTitles = (List<String>) novel.get("chapter");

            for (int i = 0; i < chapterTitles.size(); i++) {
                Map<String, Object> chapter = new HashMap<>();
                chapter.put("id", i);
                chapter.put("title", chapterTitles.get(i));
                chapter.put("novelId", novelId);
                chapter.put("novelName", novel.get("name"));
                chapter.put("isRead", i <= (int) novel.get("history"));

                chapters.add(chapter);
            }

        } catch (Exception e) {
            logger.error("获取章节列表失败", e);
        }

        return chapters;
    }

    /**
     * 获取文本
     * **/
    @GetMapping("/api/novels/{novelId}/chapters/{chapterIndex}/content")
    public Map<String, Object> getContent(@PathVariable String novelId, @PathVariable int chapterIndex) {
        logger.info("来获取文本了！");
        Map<String, Object> result = new HashMap<>();
        try {
            Map<String, Object> novel = getNovel(novelId);
            if (novel.get("success") != null) {
                result.put("success", false);
                result.put("message", "小说不存在");
                return result;
            }
            int totalChapters = (int) novel.get("totalChapters");
            if (chapterIndex < 0 || chapterIndex >= totalChapters) {
                result.put("success", false);
                result.put("message", "章节不存在");
                return result;
            }
            String chapterFilePath = novelStoragePath + novelId + "\\" + chapterIndex + ".txt";
            File chapterFile = new File(chapterFilePath);
            String content = new String(Files.readAllBytes(chapterFile.toPath()), "UTF-8");
            String chapterTitle = "";
            String chapterTitles = (String) novel.get("chapter");
            if (chapterTitles != null) {
                result.put("title", chapterTitle);
            }
            result.put("content", content);



        }catch (Exception e) {
            logger.error("获取章节内容失败", e);
            result.put("success", false);
            result.put("message", "读取失败: " + e.getMessage());
        }
        result.put("success", true);
        return result;
    }
    /**
     * 获取章节内容
     */


    @GetMapping("/{novelId}/chapters/{chapterNumber}")
    public Map<String, Object> getChapterContent(
            @PathVariable String novelId,
            @PathVariable int chapterNumber) {

        Map<String, Object> result = new HashMap<>();

        try {
            // 检查章节编号是否有效
            Map<String, Object> novel = getNovel(novelId);
            if (novel.get("success") != null) {
                result.put("success", false);
                result.put("message", "小说不存在");
                return result;
            }

            int totalChapters = (int) novel.get("totalChapters");
            if (chapterNumber < 0 || chapterNumber >= totalChapters) {
                result.put("success", false);
                result.put("message", "章节不存在");
                return result;
            }

            // 构建章节文件路径
            String chapterFilePath = novelStoragePath + novelId + "\\" + chapterNumber + ".txt";
            File chapterFile = new File(chapterFilePath);

            if (!chapterFile.exists()) {
                result.put("success", false);
                result.put("message", "章节文件不存在");
                return result;
            }

            // 读取章节内容
            String content = new String(Files.readAllBytes(chapterFile.toPath()), "UTF-8");

            // 获取章节标题
            String chapterTitle = "";
            List<String> chapterTitles = (List<String>) novel.get("chapter");
            if (chapterTitles != null && chapterNumber < chapterTitles.size()) {
                chapterTitle = chapterTitles.get(chapterNumber);
            }

            // 获取相邻章节
            int prevChapter = chapterNumber > 0 ? chapterNumber - 1 : -1;
            int nextChapter = chapterNumber < totalChapters - 1 ? chapterNumber + 1 : -1;

            result.put("success", true);
            result.put("content", content);
            result.put("title", chapterTitle);
            result.put("chapterNumber", chapterNumber);
            result.put("totalChapters", totalChapters);
            result.put("novelId", novelId);
            result.put("novelName", novel.get("name"));
            result.put("prevChapter", prevChapter);
            result.put("nextChapter", nextChapter);

        } catch (Exception e) {
            logger.error("获取章节内容失败", e);
            result.put("success", false);
            result.put("message", "读取失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 更新阅读进度
     */
    @PostMapping("/{novelId}/progress")
    public Map<String, Object> updateProgress(
            @PathVariable String novelId,
            @RequestParam int chapterNumber,
            @RequestParam(required = false) String chapterTitle) {

        Map<String, Object> result = new HashMap<>();

        try {
            logger.info("更新阅读进度: novelId={}, chapter={}", novelId, chapterNumber);

            // 读取现有的 novels.json
            File jsonFile = new File(novelDataPath);

            if (!jsonFile.exists()) {
                result.put("success", false);
                result.put("message", "novels.json 文件不存在");
                return result;
            }

            // 读取JSON内容
            String jsonContent = new String(Files.readAllBytes(jsonFile.toPath()), "UTF-8");
            JsonNode rootNode = objectMapper.readTree(jsonContent);

            if (!rootNode.isArray()) {
                result.put("success", false);
                result.put("message", "novels.json 格式错误");
                return result;
            }

            // 查找并更新小说进度
            ArrayNode novelsArray = (ArrayNode) rootNode;
            boolean novelFound = false;

            for (int i = 0; i < novelsArray.size(); i++) {
                JsonNode novelNode = novelsArray.get(i);

                if (novelNode.has("id") && novelNode.get("id").asText().equals(novelId)) {
                    // 找到小说，更新进度
                    ObjectNode novelObject = (ObjectNode) novelNode;
                    novelObject.put("history", chapterNumber);

                    novelFound = true;
                    break;
                }
            }

            if (!novelFound) {
                result.put("success", false);
                result.put("message", "小说不存在");
                return result;
            }

            // 写回文件
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, novelsArray);

            logger.info("阅读进度已更新: {} -> 第 {} 章", novelId, chapterNumber + 1);

            result.put("success", true);
            result.put("message", "进度更新成功");

        } catch (Exception e) {
            logger.error("更新阅读进度失败", e);
            result.put("success", false);
            result.put("message", "更新失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 删除小说
     */
    @DeleteMapping("/{novelId}")
    public Map<String, Object> deleteNovel(@PathVariable String novelId) {
        Map<String, Object> result = new HashMap<>();

        try {
            logger.info("删除小说: {}", novelId);

            // 1. 删除小说存储目录
            String novelDirPath = novelStoragePath + novelId + "\\";
            File novelDir = new File(novelDirPath);

            if (novelDir.exists()) {
                // 递归删除目录
                deleteDirectory(novelDir);
                logger.info("已删除小说目录: {}", novelDirPath);
            }

            // 2. 从 novels.json 中删除记录
            File jsonFile = new File(novelDataPath);

            if (jsonFile.exists()) {
                String jsonContent = new String(Files.readAllBytes(jsonFile.toPath()), "UTF-8");
                JsonNode rootNode = objectMapper.readTree(jsonContent);

                if (rootNode.isArray()) {
                    ArrayNode novelsArray = (ArrayNode) rootNode;
                    ArrayNode newArray = objectMapper.createArrayNode();

                    // 过滤掉要删除的小说
                    for (JsonNode novelNode : novelsArray) {
                        if (!novelNode.has("id") || !novelNode.get("id").asText().equals(novelId)) {
                            newArray.add(novelNode);
                        }
                    }

                    // 写回文件
                    objectMapper.writerWithDefaultPrettyPrinter().writeValue(jsonFile, newArray);
                    logger.info("已从 novels.json 中删除小说记录");
                }
            }

            result.put("success", true);
            result.put("message", "小说删除成功");

        } catch (Exception e) {
            logger.error("删除小说失败", e);
            result.put("success", false);
            result.put("message", "删除失败: " + e.getMessage());
        }

        return result;
    }

    /**
     * 获取阅读历史（返回所有小说的最新阅读进度）
     */
    @GetMapping("/history")
    public List<Map<String, Object>> getReadingHistory() {
        List<Map<String, Object>> history = new ArrayList<>();

        try {
            // 获取所有小说
            List<Map<String, Object>> allNovels = getAllNovels();

            // 过滤出有阅读进度的
            for (Map<String, Object> novel : allNovels) {
                int historyChapter = (int) novel.get("history");

                if (historyChapter > 0) {
                    Map<String, Object> historyItem = new HashMap<>();
                    historyItem.put("novelId", novel.get("id"));
                    historyItem.put("novelTitle", novel.get("name"));
                    historyItem.put("chapterId", historyChapter);

                    // 获取章节标题
                    List<String> chapters = (List<String>) novel.get("chapter");
                    if (chapters != null && historyChapter < chapters.size()) {
                        historyItem.put("chapterTitle", chapters.get(historyChapter));
                    } else {
                        historyItem.put("chapterTitle", "第" + (historyChapter + 1) + "章");
                    }

                    // 查找阅读时间戳（如果有的话）
                    // 注意：我们的数据结构目前没有存储时间戳，可能需要修改
                    // 这里使用当前时间作为示例
                    historyItem.put("timestamp", System.currentTimeMillis());

                    history.add(historyItem);
                }
            }

            // 按时间倒序排序（由于没有时间戳，按ID排序）
            history.sort((a, b) -> {
                String idA = (String) a.get("novelId");
                String idB = (String) b.get("novelId");
                return idB.compareTo(idA);
            });

        } catch (Exception e) {
            logger.error("获取阅读历史失败", e);
        }

        return history;
    }

    /**
     * 搜索小说
     */
    @GetMapping("/search")
    public List<Map<String, Object>> searchNovels(@RequestParam String keyword) {
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            if (keyword == null || keyword.trim().isEmpty()) {
                return getAllNovels();
            }

            String searchTerm = keyword.toLowerCase().trim();
            List<Map<String, Object>> allNovels = getAllNovels();

            for (Map<String, Object> novel : allNovels) {
                String novelName = (String) novel.get("name");

                if (novelName.toLowerCase().contains(searchTerm)) {
                    results.add(novel);
                }
            }

            logger.info("搜索 '{}' 找到 {} 本小说", keyword, results.size());

        } catch (Exception e) {
            logger.error("搜索小说失败", e);
        }

        return results;
    }

    /**
     * 获取小说的统计信息
     */
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        Map<String, Object> stats = new HashMap<>();

        try {
            List<Map<String, Object>> allNovels = getAllNovels();

            int totalNovels = allNovels.size();
            int totalChapters = 0;
            int totalReadProgress = 0;

            for (Map<String, Object> novel : allNovels) {
                int chapters = (int) novel.get("totalChapters");
                int history = (int) novel.get("history");

                totalChapters += chapters;
                if (history > 0) {
                    totalReadProgress++;
                }
            }

            stats.put("totalNovels", totalNovels);
            stats.put("totalChapters", totalChapters);
            stats.put("novelsWithProgress", totalReadProgress);
            stats.put("avgChaptersPerNovel", totalNovels > 0 ? totalChapters / totalNovels : 0);

        } catch (Exception e) {
            logger.error("获取统计信息失败", e);
        }

        return stats;
    }

    /**
     * 递归删除目录
     */
    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteDirectory(child);
                }
            }
        }
        dir.delete();
    }
}