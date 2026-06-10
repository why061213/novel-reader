package com.novelreader.model;

public class ChapterInfo {
    private int index;      // 章节索引（从0开始）
    private String title;   // 章节标题
    private String content; // 章节内容（包含标题）
    private int wordCount;  // 字数

    // 无参构造函数
    public ChapterInfo() {}

    // 有参构造函数
    public ChapterInfo(int index, String title, String content, int wordCount) {
        this.index = index;
        this.title = title;
        this.content = content;
        this.wordCount = wordCount;
    }

    // getter 和 setter 方法
    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
}