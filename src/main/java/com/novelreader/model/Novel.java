package com.novelreader.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Novel {
    private String id;
    private String name;
    private String originalFilename;
    private String storagePath;  // 添加这个字段
    private int totalChapters;
    private long totalWords;
    private String coverImage;
    private Integer history;
    private String[] chapters;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastReadAt;

    private Integer lastReadChapter;
    private String lastReadChapterTitle;

    @JsonIgnore
    private List<Chapter> chapter;

    public Novel() {
        this.id = UUID.randomUUID().toString();
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Integer getHistory() { return this.history; }
    public void setHistory(Integer history) { this.history = history; }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String[] getChapters() { return this.chapters; }
    public void setChapters(String[] Chapters) { this.chapters = Chapters; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }

    // 添加 storagePath 的 getter 和 setter
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }

    public int getTotalChapters() { return totalChapters; }
    public void setTotalChapters(int totalChapters) { this.totalChapters = totalChapters; }

    public long getTotalWords() { return totalWords; }
    public void setTotalWords(long totalWords) { this.totalWords = totalWords; }

    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getLastReadAt() { return lastReadAt; }
    public void setLastReadAt(LocalDateTime lastReadAt) { this.lastReadAt = lastReadAt; }

    public Integer getLastReadChapter() { return lastReadChapter; }
    public void setLastReadChapter(Integer lastReadChapter) { this.lastReadChapter = lastReadChapter; }

    public String getLastReadChapterTitle() { return lastReadChapterTitle; }
    public void setLastReadChapterTitle(String lastReadChapterTitle) { this.lastReadChapterTitle = lastReadChapterTitle; }

    public List<Chapter> getChapter() { return chapter; }
    public void setChapter(List<Chapter> chapters) { this.chapter = chapters; }
}