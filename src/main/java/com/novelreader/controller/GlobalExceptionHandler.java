package com.novelreader.controller;

import com.novelreader.model.ImportResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ImportResult> handleException(Exception e) {
        logger.error("服务器内部错误", e);
        ImportResult result = new ImportResult(false, "服务器内部错误: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ImportResult> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        logger.warn("文件大小超过限制", e);
        ImportResult result = new ImportResult(false, "文件大小超过限制");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(result);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ImportResult> handleIllegalArgumentException(IllegalArgumentException e) {
        logger.warn("参数错误", e);
        ImportResult result = new ImportResult(false, "参数错误: " + e.getMessage());
        return ResponseEntity.badRequest().body(result);
    }
}