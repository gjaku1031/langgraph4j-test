package com.example.langgraph4j.examples.messagegraph.controller;

import com.example.langgraph4j.examples.messagegraph.model.GraphState;
import com.example.langgraph4j.examples.messagegraph.service.MessageGraphService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * MessageGraph 예제를 위한 REST API 컨트롤러
 * 
 * Python 예제의 MessageGraph 기능을 웹 API로 제공합니다.
 * 품질 제어가 있는 RAG 시스템을 구현합니다.
 * 
 * @author Claude AI Assistant
 * @since 2025-07-31
 */
@Slf4j
@RestController
@RequestMapping("/api/examples/messagegraph")
@RequiredArgsConstructor
public class MessageGraphController {

    private final MessageGraphService messageGraphService;

    /**
     * MessageGraph 실행 (품질 제어가 있는 RAG)
     * 
     * @param request 사용자 질문이 포함된 요청
     * @return 품질 제어를 거친 대화 결과
     */
    @PostMapping("/chat")
    public ResponseEntity<MessageGraphResponse> executeChatWithQualityControl(
            @RequestBody MessageGraphRequest request) {
        log.info("MessageGraph 실행 요청: {}", request.getQuery());
        
        try {
            GraphState result = messageGraphService.executeMessageGraph(request.getQuery());
            
            MessageGraphResponse response = MessageGraphResponse.builder()
                .success(true)
                .query(request.getQuery())
                .state(result)
                .finalAnswer(result.getLastAiMessage() != null ? 
                    result.getLastAiMessage().getContent() : "응답을 생성할 수 없습니다.")
                .qualityScore(result.getGrade())
                .generations(result.getNumGeneration())
                .documentsFound(result.getDocuments() != null ? result.getDocuments().size() : 0)
                .summary(createSummary(result))
                .timestamp(LocalDateTime.now())
                .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("MessageGraph 실행 중 오류 발생", e);
            
            MessageGraphResponse errorResponse = MessageGraphResponse.builder()
                .success(false)
                .query(request.getQuery())
                .error("MessageGraph 실행 중 오류가 발생했습니다: " + e.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
            
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * MessageGraph 설정 정보 조회
     * 
     * @return 현재 설정 정보
     */
    @GetMapping("/config")
    public ResponseEntity<String> getConfiguration() {
        log.info("MessageGraph 설정 정보 조회");
        return ResponseEntity.ok(messageGraphService.getConfiguration());
    }

    /**
     * 대화 요약 생성
     */
    private String createSummary(GraphState state) {
        if (state.getMessages() == null || state.getMessages().isEmpty()) {
            return "대화가 없습니다.";
        }
        
        int messageCount = state.getMessages().size();
        String qualityInfo = state.getGrade() != null ? 
            String.format("품질: %.2f", state.getGrade()) : "품질: 미평가";
        
        String retryInfo = state.getNumGeneration() > 1 ? 
            String.format("(%d회 재시도)", state.getNumGeneration() - 1) : "";
        
        return String.format("메시지 %d개, %s %s", messageCount, qualityInfo, retryInfo);
    }

    /**
     * MessageGraph 요청 모델
     */
    @Data
    public static class MessageGraphRequest {
        private String query;
    }

    /**
     * MessageGraph 응답 모델
     */
    @Data
    @lombok.Builder
    public static class MessageGraphResponse {
        private boolean success;
        private String query;
        private GraphState state;
        private String finalAnswer;
        private Double qualityScore;
        private Integer generations;
        private Integer documentsFound;
        private String summary;
        private String error;
        private LocalDateTime timestamp;
    }
}