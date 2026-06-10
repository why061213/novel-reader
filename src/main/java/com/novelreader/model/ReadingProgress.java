package com.novelreader.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class ReadingProgress {
    private String novelId;
    private String novelTitle;
    private int chapterNumber;
    private String chapterTitle;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime readAt;

    public ReadingProgress() {
        this.readAt = LocalDateTime.now();
    }

    public ReadingProgress(String novelId, String novelTitle, int chapterNumber, String chapterTitle) {
        this();
        this.novelId = novelId;
        this.novelTitle = novelTitle;
        this.chapterNumber = chapterNumber;
        this.chapterTitle = chapterTitle;
    }

    // Getters and Setters
    public String getNovelId() { return novelId; }
    public void setNovelId(String novelId) { this.novelId = novelId; }

    public String getNovelTitle() { return novelTitle; }
    public void setNovelTitle(String novelTitle) { this.novelTitle = novelTitle; }

    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int chapterNumber) { this.chapterNumber = chapterNumber; }

    public String getChapterTitle() { return chapterTitle; }
    public void setChapterTitle(String chapterTitle) { this.chapterTitle = chapterTitle; }

    public LocalDateTime getReadAt() { return readAt; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
}