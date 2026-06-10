package com.novelreader.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novelreader.model.Novel;
import com.novelreader.model.ReadingProgress;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DataService {

    private static final Logger logger = LoggerFactory.getLogger(DataService.class);

    private final Map<String, Novel> novels = new ConcurrentHashMap<>();
    private final List<ReadingProgress> readingHistory = new ArrayList<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${novel.data.base-path:./novel-data}")
    private String dataBasePath;

    private static final String NOVELS_FILE = "novels.json";
//    private static final String HISTORY_FILE = "history.json";

    @PostConstruct
    public void init() {
        try {
            // 创建数据目录
            Files.createDirectories(Paths.get(dataBasePath));

            // 加载小说数据
            loadNovels();

            // 加载历史记录
//            loadHistory();

            logger.info("数据服务初始化完成，加载了 {} 本小说，{} 条历史记录",
                    novels.size(), readingHistory.size());
        } catch (IOException e) {
            logger.error("初始化数据服务失败", e);
        }
    }

    /**
     * 加载小说数据
     */
    private synchronized void loadNovels() throws IOException {
        File novelsFile = new File(dataBasePath, NOVELS_FILE);
        if (novelsFile.exists()) {
            Novel[] novelArray = objectMapper.readValue(novelsFile, Novel[].class);
            novels.clear();
            for (Novel novel : novelArray) {
                novels.put(novel.getId(), novel);
            }
            logger.info("从文件加载了 {} 本小说", novels.size());
        } else {
            logger.info("未找到小说数据文件，创建新的数据文件");
            saveNovels();
        }
    }

    /**
     * 保存小说数据
     */
    private synchronized void saveNovels() throws IOException {
        File novelsFile = new File(dataBasePath, NOVELS_FILE);
        List<Novel> novelList = new ArrayList<>(novels.values());
        objectMapper.writeValue(novelsFile, novelList);
    }

    /**
     * 加载历史记录
     */
//    private synchronized void loadHistory() throws IOException {
//        File historyFile = new File(dataBasePath, HISTORY_FILE);
//        if (historyFile.exists()) {
//            ReadingProgress[] historyArray = objectMapper.readValue(historyFile, ReadingProgress[].class);
//            readingHistory.clear();
//            readingHistory.addAll(Arrays.asList(historyArray));
//            // 按阅读时间倒序排序
//            readingHistory.sort((a, b) -> b.getReadAt().compareTo(a.getReadAt()));
//            logger.info("从文件加载了 {} 条历史记录", readingHistory.size());
//        }
//    }

    /**
     * 保存历史记录
     */
//    private synchronized void saveHistory() throws IOException {
//        File historyFile = new File(dataBasePath, HISTORY_FILE);
//        objectMapper.writeValue(historyFile, readingHistory);
//    }

    /**
     * 获取所有小说
     */
    public List<Novel> getAllNovels() {
        return new ArrayList<>(novels.values());
    }

    /**
     * 根据ID获取小说
     */
    public Novel getNovelById(String novelId) {
        return novels.get(novelId);
    }

    /**
     * 添加小说
     */
    public void addNovel(Novel novel) throws IOException {
        novels.put(novel.getId(), novel);
        saveNovels();
    }

    /**
     * 更新小说
     */
    public void updateNovel(Novel novel) throws IOException {
        novels.put(novel.getId(), novel);
        saveNovels();
    }

    /**
     * 删除小说
     */
    public boolean deleteNovel(String novelId) throws IOException {
        Novel removed = novels.remove(novelId);
        if (removed != null) {
            // 删除相关历史记录
            readingHistory.removeIf(progress -> progress.getNovelId().equals(novelId));
            saveNovels();
//            saveHistory();
            return true;
        }
        return false;
    }

    /**
     * 添加阅读进度
     */
    public void addReadingProgress(ReadingProgress progress) throws IOException {
        // 移除同一小说的旧记录
        readingHistory.removeIf(p -> p.getNovelId().equals(progress.getNovelId()));

        // 添加到历史记录
        readingHistory.add(0, progress);

        // 保持最多20条记录
        if (readingHistory.size() > 20) {
            readingHistory.remove(readingHistory.size() - 1);
        }

//        saveHistory();
    }

    /**
     * 获取阅读历史
     */
    public List<ReadingProgress> getReadingHistory() {
        return new ArrayList<>(readingHistory);
    }

    /**
     * 搜索小说
     */
    public List<Novel> searchNovels(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllNovels();
        }

        String searchTerm = keyword.toLowerCase().trim();
        return novels.values().stream()
                .filter(novel -> novel.getName().toLowerCase().contains(searchTerm))
                .collect(Collectors.toList());
    }
}