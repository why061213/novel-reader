package com.novelreader.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class Chapter {
    private int chapterNumber;
    private String title;
    private String filePath;
    private int wordCount;

    @JsonIgnore
    private String content;

    public Chapter() {}

    public Chapter(int chapterNumber, String title, String content, String filePath) {
        this.chapterNumber = chapterNumber;
        this.title = title;
        this.content = content;
        this.filePath = filePath;
        this.wordCount = content.length();
    }

    // Getters and Setters
    public int getChapterNumber() { return chapterNumber; }
    public void setChapterNumber(int chapterNumber) { this.chapterNumber = chapterNumber; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }

    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}