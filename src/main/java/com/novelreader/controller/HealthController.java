//package com.novelreader.controller;
//
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.HashMap;
//import java.util.Map;
//
//@RestController
//public class HealthController {
//
//    @GetMapping("/api/health")
//    public Map<String, Object> health() {
//        Map<String, Object> result = new HashMap<>();
//        result.put("status", "UP");
//        result.put("service", "Novel Reader API");
//        result.put("timestamp", System.currentTimeMillis());
//        result.put("version", "1.0.0");
//        return result;
//    }
//
//    @GetMapping("/ping")
//    public String ping() {
//        return "pong";
//    }
//
////    @GetMapping("/")
////    public Map<String, String> home() {
////        Map<String, String> result = new HashMap<>();
////        result.put("message", "Novel Reader API");
////        result.put("endpoint", "/api/novels/upload");
////        result.put("status", "running");
////        return result;
////    }
//}

package com.novelreader.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("service", "文件上传测试");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }

    @GetMapping("/ping")
    public String ping() {
        return "pong";
    }
}