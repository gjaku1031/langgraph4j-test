package com.example.langgraph4j.examples.reactmemory.controller;

import com.example.langgraph4j.examples.reactmemory.model.ReActState;
import com.example.langgraph4j.examples.reactmemory.service.MemoryManager;
import com.example.langgraph4j.examples.reactmemory.service.ReActAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

/**
 * ReAct + Memory 시스템의 REST API 컨트롤러
 * 
 * Python 예제의 ReAct 패턴을 Java로 구현한 컨트롤러입니다.
 * 메모리를 활용한 대화형 AI 에이전트 기능을 제공합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Slf4j
@RestController
@RequestMapping("/api/react-memory")
@RequiredArgsConstructor
public class ReActMemoryController {

    private final ReActAgentService reActAgentService;
    private final MemoryManager memoryManager;

    /**
     * ReAct 에이전트와 대화하기 (메모리 지원)
     * 
     * @param request 사용자 요청
     * @return ReAct 처리 결과
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        log.info("=== ReAct 메모리 채팅 요청: {} (스레드: {}) ===", 
            request.getMessage(), request.getThreadId());
        
        try {
            // ReAct 에이전트 실행
            ReActState result = reActAgentService.executeReActAgent(
                request.getMessage(), 
                request.getThreadId()
            );
            
            // 응답 생성
            Map<String, Object> response = Map.of(
                "threadId", result.getThreadId(),
                "currentStep", result.getCurrentStep(),
                "messageCount", result.getMessages().size(),
                "toolCallCount", result.getToolCalls().size(),
                "reasoning", result.getReasoning() != null ? result.getReasoning() : "",
                "action", result.getAction() != null ? result.getAction() : "",
                "observation", result.getObservation() != null ? result.getObservation() : "",
                "summary", result.getSummary(),
                "timestamp", result.getTimestamp(),
                "lastMessage", result.getLastMessage() != null ? 
                    result.getLastMessage().getContent() : "메시지 없음"
            );
            
            log.info("ReAct 처리 완료: {}", result.getSummary());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("ReAct 처리 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "ReAct 처리 실패: " + e.getMessage(),
                    "success", false
                ));
        }
    }

    /**
     * 새로운 대화 스레드 생성
     * 
     * @return 새로 생성된 스레드 ID
     */
    @PostMapping("/threads")
    public ResponseEntity<Map<String, Object>> createThread() {
        try {
            String threadId = memoryManager.createThread();
            
            Map<String, Object> response = Map.of(
                "threadId", threadId,
                "message", "새로운 대화 스레드가 생성되었습니다.",
                "success", true
            );
            
            log.info("새로운 스레드 생성: {}", threadId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("스레드 생성 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "스레드 생성 실패: " + e.getMessage(),
                    "success", false
                ));
        }
    }

    /**
     * 특정 스레드의 대화 히스토리 조회
     * 
     * @param threadId 스레드 ID
     * @return 대화 히스토리
     */
    @GetMapping("/threads/{threadId}/history")
    public ResponseEntity<Map<String, Object>> getConversationHistory(
            @PathVariable String threadId) {
        
        try {
            ReActState state = memoryManager.getConversationHistory(threadId);
            
            Map<String, Object> response = Map.of(
                "threadId", state.getThreadId(),
                "messages", state.getMessages(),
                "toolCalls", state.getToolCalls(),
                "currentStep", state.getCurrentStep(),
                "summary", state.getSummary(),
                "timestamp", state.getTimestamp()
            );
            
            log.info("대화 히스토리 조회: {} (메시지 {}개)", 
                threadId, state.getMessages().size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("대화 히스토리 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "대화 히스토리 조회 실패: " + e.getMessage(),
                    "success", false
                ));
        }
    }

    /**
     * 모든 활성 스레드 목록 조회
     * 
     * @return 스레드 목록
     */
    @GetMapping("/threads")
    public ResponseEntity<Map<String, Object>> getAllThreads() {
        try {
            Set<String> threadIds = memoryManager.getAllThreadIds();
            
            Map<String, Object> response = Map.of(
                "threads", threadIds,
                "count", threadIds.size(),
                "memoryStatus", memoryManager.getMemoryStatus()
            );
            
            log.info("전체 스레드 조회: {}개", threadIds.size());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("스레드 목록 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "스레드 목록 조회 실패: " + e.getMessage(),
                    "success", false
                ));
        }
    }

    /**
     * 특정 스레드 삭제
     * 
     * @param threadId 삭제할 스레드 ID
     * @return 삭제 결과
     */
    @DeleteMapping("/threads/{threadId}")
    public ResponseEntity<Map<String, Object>> deleteThread(@PathVariable String threadId) {
        try {
            memoryManager.deleteThread(threadId);
            
            Map<String, Object> response = Map.of(
                "message", "스레드가 성공적으로 삭제되었습니다.",
                "threadId", threadId,
                "success", true
            );
            
            log.info("스레드 삭제: {}", threadId);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("스레드 삭제 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "스레드 삭제 실패: " + e.getMessage(),
                    "success", false
                ));
        }
    }

    /**
     * 사용 가능한 도구 목록 조회
     * 
     * @return 도구 목록
     */
    @GetMapping("/tools")
    public ResponseEntity<Map<String, Object>> getAvailableTools() {
        try {
            Map<String, Object> response = Map.of(
                "tools", reActAgentService.getAvailableTools(),
                "description", Map.of(
                    "search_menu", "레스토랑 메뉴 정보 검색",
                    "search_wine", "와인 정보 및 페어링 검색",
                    "search_web", "웹에서 최신 정보 검색"
                )
            );
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("도구 목록 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "도구 목록 조회 실패: " + e.getMessage(),
                    "success", false
                ));
        }
    }

    /**
     * 메모리 정리 (오래된 스레드 삭제)
     * 
     * @param hoursToKeep 유지할 시간 (기본값: 24시간)
     * @return 정리 결과
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupMemory(
            @RequestParam(defaultValue = "24") int hoursToKeep) {
        
        try {
            memoryManager.cleanupOldMemories(hoursToKeep);
            
            Map<String, Object> response = Map.of(
                "message", String.format("%d시간 이전 메모리가 정리되었습니다.", hoursToKeep),
                "currentStatus", memoryManager.getMemoryStatus(),
                "success", true
            );
            
            log.info("메모리 정리 완료: {}시간 기준", hoursToKeep);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("메모리 정리 중 오류 발생", e);
            return ResponseEntity.internalServerError()
                .body(Map.of(
                    "error", "메모리 정리 실패: " + e.getMessage(),
                    "success", false
                ));
        }
    }

    /**
     * 간단한 테스트 엔드포인트
     * 
     * @return 테스트 응답
     */
    @GetMapping("/test")
    public ResponseEntity<Map<String, Object>> test() {
        Map<String, Object> response = Map.of(
            "message", "ReAct Memory 시스템이 정상적으로 작동 중입니다.",
            "availableTools", reActAgentService.getAvailableTools(),
            "memoryStatus", memoryManager.getMemoryStatus(),
            "timestamp", java.time.LocalDateTime.now()
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * 채팅 요청 DTO
     */
    public static class ChatRequest {
        private String message;
        private String threadId;

        // 기본 생성자
        public ChatRequest() {}

        // 매개변수 생성자
        public ChatRequest(String message, String threadId) {
            this.message = message;
            this.threadId = threadId;
        }

        // Getter/Setter
        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getThreadId() {
            return threadId;
        }

        public void setThreadId(String threadId) {
            this.threadId = threadId;
        }
    }
}