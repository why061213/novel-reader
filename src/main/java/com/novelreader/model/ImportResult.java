package com.novelreader.model;

public class ImportResult {
    private boolean success;
    private String message;
    private String novelId;
    private String novelTitle;
    private int totalChapters;
    private long processingTime;

    public ImportResult(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // Getters and Setters
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getNovelId() { return novelId; }
    public void setNovelId(String novelId) { this.novelId = novelId; }

    public String getNovelTitle() { return novelTitle; }
    public void setNovelTitle(String novelTitle) { this.novelTitle = novelTitle; }

    public int getTotalChapters() { return totalChapters; }
    public void setTotalChapters(int totalChapters) { this.totalChapters = totalChapters; }

    public long getProcessingTime() { return processingTime; }
    public void setProcessingTime(long processingTime) { this.processingTime = processingTime; }
}