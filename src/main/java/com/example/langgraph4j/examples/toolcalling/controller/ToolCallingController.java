package com.example.langgraph4j.examples.toolcalling.controller;

import com.example.langgraph4j.examples.toolcalling.service.ToolCallingService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Tool Calling 예제 REST API 컨트롤러
 * 
 * 도구 호출 기능을 테스트할 수 있는 API 엔드포인트를 제공합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Slf4j
@RestController
@RequestMapping("/api/examples/toolcalling")
@RequiredArgsConstructor
public class ToolCallingController {

    private final ToolCallingService toolCallingService;

    /**
     * 기본 도구 호출 테스트
     * 
     * @param request 사용자 질문을 포함한 요청
     * @return AI 응답
     */
    @PostMapping("/basic")
    public ResponseEntity<ToolCallingResponse> basicToolCalling(@RequestBody ToolCallingRequest request) {
        log.info("기본 도구 호출 요청: {}", request.getQuery());
        
        try {
            String response = toolCallingService.processWithTools(request.getQuery());
            
            return ResponseEntity.ok(ToolCallingResponse.builder()
                .success(true)
                .query(request.getQuery())
                .response(response)
                .timestamp(LocalDateTime.now())
                .build());
                
        } catch (Exception e) {
            log.error("도구 호출 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ToolCallingResponse.builder()
                    .success(false)
                    .query(request.getQuery())
                    .response("오류가 발생했습니다: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * Few-shot 예제를 사용한 도구 호출
     * 
     * @param request 사용자 질문을 포함한 요청
     * @return AI 응답
     */
    @PostMapping("/few-shot")
    public ResponseEntity<ToolCallingResponse> fewShotToolCalling(@RequestBody ToolCallingRequest request) {
        log.info("Few-shot 도구 호출 요청: {}", request.getQuery());
        
        try {
            String response = toolCallingService.processWithFewShotExamples(request.getQuery());
            
            return ResponseEntity.ok(ToolCallingResponse.builder()
                .success(true)
                .query(request.getQuery())
                .response(response)
                .timestamp(LocalDateTime.now())
                .build());
                
        } catch (Exception e) {
            log.error("Few-shot 도구 호출 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ToolCallingResponse.builder()
                    .success(false)
                    .query(request.getQuery())
                    .response("오류가 발생했습니다: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * 메모리를 사용한 대화형 도구 호출
     * 
     * @param request 사용자 질문을 포함한 요청
     * @return AI 응답
     */
    @PostMapping("/with-memory")
    public ResponseEntity<ToolCallingResponse> memoryToolCalling(@RequestBody ToolCallingRequest request) {
        log.info("메모리 기반 도구 호출 요청: {}", request.getQuery());
        
        try {
            String response = toolCallingService.processWithMemory(request.getQuery());
            
            return ResponseEntity.ok(ToolCallingResponse.builder()
                .success(true)
                .query(request.getQuery())
                .response(response)
                .timestamp(LocalDateTime.now())
                .build());
                
        } catch (Exception e) {
            log.error("메모리 기반 도구 호출 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(ToolCallingResponse.builder()
                    .success(false)
                    .query(request.getQuery())
                    .response("오류가 발생했습니다: " + e.getMessage())
                    .timestamp(LocalDateTime.now())
                    .build());
        }
    }

    /**
     * 사용 가능한 도구 목록 조회
     * 
     * @return 도구 목록
     */
    @GetMapping("/tools")
    public ResponseEntity<Map<String, Object>> getAvailableTools() {
        Map<String, Object> response = new HashMap<>();
        response.put("tools", toolCallingService.getAvailableTools());
        response.put("timestamp", LocalDateTime.now());
        
        return ResponseEntity.ok(response);
    }

    /**
     * 도구 호출 요청 모델
     */
    @Data
    public static class ToolCallingRequest {
        private String query;
    }

    /**
     * 도구 호출 응답 모델
     */
    @Data
    @lombok.Builder
    public static class ToolCallingResponse {
        private boolean success;
        private String query;
        private String response;
        private LocalDateTime timestamp;
    }
}