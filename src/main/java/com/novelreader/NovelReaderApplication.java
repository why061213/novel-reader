package com.novelreader;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NovelReaderApplication {

    public static void main(String[] args) {
        SpringApplication.run(NovelReaderApplication.class, args);
        System.out.println("========================================");
        System.out.println("文件上传测试服务已启动！");
        System.out.println("访问地址：http://localhost:8080");
        System.out.println("API端点：");
        System.out.println("  GET  /health           - 健康检查");
        System.out.println("  GET  /ping             - 简单测试");
        System.out.println("  GET  /api/upload/test  - 上传测试连接");
        System.out.println("  POST /api/upload/file  - 文件上传（唯一文件名）");
        System.out.println("  POST /api/upload/simple - 文件上传（原始文件名）");
        System.out.println("========================================");
    }
}