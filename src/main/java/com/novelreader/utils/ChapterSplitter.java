package com.novelreader.utils;

import com.novelreader.model.ChapterInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChapterSplitter {

    // 正则表达式匹配章节标题（支持汉字数字和阿拉伯数字）
    private static final Pattern CHAPTER_PATTERN = Pattern.compile(
            "(第[零一二三四五六七八九十百千万两\\d]+章[^\\n]*)([\\s\\S]*?)(?=第[零一二三四五六七八九十百千万两\\d]+章|$)"
    );

    // 汉字数字转阿拉伯数字的映射
    private static final String[] CHINESE_NUMBERS = {"零", "一", "二", "三", "四", "五", "六", "七", "八", "九", "十"};
    private static final String[] CHINESE_UNITS = {"十", "百", "千", "万"};

    /**
     * 分割小说内容为章节
     */
    public static List<ChapterInfo> splitChapters(String content) {
        List<ChapterInfo> chapters = new ArrayList<>();

        // 使用正则匹配所有章节
        Matcher matcher = CHAPTER_PATTERN.matcher(content);

        int chapterIndex = 0;
        int lastMatchEnd = 0;

        while (matcher.find()) {
            // 获取章节标题（第一组）
            String chapterTitle = matcher.group(1).trim();
            // 获取章节内容（标题+正文，第二组）
            String chapterContent = matcher.group(2);

            // 组合标题和内容
            String fullContent = chapterTitle + chapterContent;

            ChapterInfo chapter = new ChapterInfo();
            chapter.setIndex(chapterIndex);
            chapter.setTitle(chapterTitle);
            chapter.setContent(fullContent); // 保留标题在内容中
            chapter.setWordCount(fullContent.length());

            chapters.add(chapter);
            chapterIndex++;

            lastMatchEnd = matcher.end();
        }

        // 处理开头没有匹配到章节的内容（作为第一章）
        if (lastMatchEnd > 0 && lastMatchEnd < content.length()) {
            // 剩余内容作为最后一个章节的一部分
            if (!chapters.isEmpty()) {
                ChapterInfo lastChapter = chapters.get(chapters.size() - 1);
                String updatedContent = lastChapter.getContent() + content.substring(lastMatchEnd);
                lastChapter.setContent(updatedContent);
                lastChapter.setWordCount(updatedContent.length());
            }
        }

        // 如果没有匹配到任何章节，将整个内容作为一章
        if (chapters.isEmpty() && !content.trim().isEmpty()) {
            ChapterInfo chapter = new ChapterInfo();
            chapter.setIndex(0);
            chapter.setTitle("第一章");
            chapter.setContent("第一章\n\n" + content); // 添加默认标题
            chapter.setWordCount(content.length());
            chapters.add(chapter);
        }

        return chapters;
    }

    /**
     * 按固定长度分割（备用方案）
     */
    private static List<ChapterInfo> splitByFixedLength(String content) {
        List<ChapterInfo> chapters = new ArrayList<>();

        if (content == null || content.isEmpty()) {
            return chapters;
        }

        int chapterLength = 3000; // 每章约3000字
        int totalLength = content.length();

        for (int i = 0; i < totalLength; i += chapterLength) {
            int end = Math.min(i + chapterLength, totalLength);
            String chapterContent = content.substring(i, end).trim();

            if (!chapterContent.isEmpty()) {
                ChapterInfo chapter = new ChapterInfo();
                chapter.setIndex(chapters.size());
                chapter.setTitle("第" + (chapters.size() + 1) + "章");
                chapter.setContent(chapter.getTitle() + "\n\n" + chapterContent); // 添加标题
                chapter.setWordCount(chapterContent.length());

                chapters.add(chapter);
            }
        }

        return chapters;
    }

    /**
     * 汉字数字转阿拉伯数字
     */
    public static int chineseToArabic(String chineseNumber) {
        if (chineseNumber == null || chineseNumber.isEmpty()) {
            return 0;
        }

        // 如果是阿拉伯数字，直接转换
        if (chineseNumber.matches("\\d+")) {
            return Integer.parseInt(chineseNumber);
        }

        try {
            // 简单的汉字数字转换（支持一到九十九）
            int result = 0;
            int temp = 0;

            for (int i = 0; i < chineseNumber.length(); i++) {
                char c = chineseNumber.charAt(i);
                int digit = charToDigit(c);

                if (digit == 10) { // "十"
                    if (temp == 0) {
                        temp = 1;
                    }
                    result += temp * 10;
                    temp = 0;
                } else if (digit == 100) { // "百"
                    result += temp * 100;
                    temp = 0;
                } else if (digit == 1000) { // "千"
                    result += temp * 1000;
                    temp = 0;
                } else if (digit == 10000) { // "万"
                    result += temp * 10000;
                    temp = 0;
                } else {
                    temp = digit;
                }
            }

            result += temp;
            return result;
        } catch (Exception e) {
            return 0;
        }
    }

    private static int charToDigit(char c) {
        switch (c) {
            case '零': return 0;
            case '一': return 1;
            case '二': return 2;
            case '三': return 3;
            case '四': return 4;
            case '五': return 5;
            case '六': return 6;
            case '七': return 7;
            case '八': return 8;
            case '九': return 9;
            case '十': return 10;
            case '百': return 100;
            case '千': return 1000;
            case '万': return 10000;
            case '两': return 2; // "两"也代表2
            default:
                if (c >= '0' && c <= '9') {
                    return Character.getNumericValue(c);
                }
                return 0;
        }
    }
}